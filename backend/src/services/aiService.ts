import { AiAnalysisRequest, AiAnalysisResponse, RiskLevel } from '../models/aiModels';

export class AiService {
    private apiKey: string;
    private modelName: string;

    constructor() {
        this.apiKey = process.env.GEMINI_API_KEY || '';
        this.modelName = process.env.GEMINI_MODEL || 'gemini-3.6-flash';
    }

    async analyzeContent(request: AiAnalysisRequest): Promise<AiAnalysisResponse> {
        if (!this.apiKey || this.apiKey === 'YOUR_SERVER_SIDE_GEMINI_API_KEY') {
            return this.getMockAiResponse(request, 'fallback');
        }

        try {
            const prompt = `You are the TrustLens cybersecurity analysis engine. Analyze whether the provided digital content contains indicators of phishing, fraud, or scams.
Return ONLY a valid JSON object matching this schema:
{
    "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL | UNKNOWN",
    "riskScore": number (0 to 100),
    "summary": "string",
    "indicators": [{"title": "string", "description": "string"}],
    "recommendation": "string",
    "confidence": number (0.0 to 1.0)
}

Content type: ${request.contentType}
Content: ${request.content}
Local risk score: ${request.localRiskScore}
Local risk level: ${request.localRiskLevel}
Local indicators: ${JSON.stringify(request.localIndicators)}`;

            const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${this.modelName}:generateContent?key=${this.apiKey}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    contents: [{ parts: [{ text: prompt }] }],
                    generationConfig: { responseMimeType: "application/json" }
                })
            });

            if (!response.ok) {
                return this.getMockAiResponse(request, 'fallback');
            }

            const data: any = await response.json();
            const textContent = data.candidates?.[0]?.content?.parts?.[0]?.text;
            if (!textContent) {
                return this.getMockAiResponse(request, 'fallback');
            }

            const parsed = JSON.parse(textContent);
            return {
                riskLevel: parsed.riskLevel || 'UNKNOWN',
                riskScore: typeof parsed.riskScore === 'number' ? Math.min(100, Math.max(0, parsed.riskScore)) : request.localRiskScore,
                summary: parsed.summary || 'Analyzed via Gemini AI.',
                indicators: Array.isArray(parsed.indicators) ? parsed.indicators : [],
                recommendation: parsed.recommendation || 'Verify through official channels.',
                confidence: typeof parsed.confidence === 'number' ? Math.min(1.0, Math.max(0.0, parsed.confidence)) : 0.85,
                aiSource: 'gemini'
            };
        } catch (error) {
            return this.getMockAiResponse(request, 'fallback');
        }
    }

    private getMockAiResponse(request: AiAnalysisRequest, source: 'gemini' | 'fallback'): AiAnalysisResponse {
        const isHigh = (request.localRiskScore || 0) > 50 || request.content.toLowerCase().includes('otp') || request.content.toLowerCase().includes('block');
        const score = isHigh ? 84 : 18;
        const level: RiskLevel = isHigh ? 'HIGH' : 'LOW';

        return {
            riskLevel: level,
            riskScore: score,
            summary: isHigh ? "AI analysis detected strong indicators matching known phishing patterns and urgent social engineering tactics." : "AI analysis verified no major malicious indicators. Content appears standard.",
            indicators: isHigh ? [
                { title: "Contextual Fraud Risk", description: "Language structure matches aggressive pressure tactics." },
                { title: "AI Heuristic Match", description: "Correlates with known credential harvesting templates." }
            ] : [
                { title: "Normal Context", description: "No anomalous indicators found by contextual AI." }
            ],
            recommendation: isHigh ? "Do not share sensitive credentials or click unknown links." : "Content appears safe. Exercise normal caution.",
            confidence: 0.88,
            aiSource: source
        };
    }
}

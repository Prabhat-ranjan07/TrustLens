export type ContentType = 'URL' | 'MESSAGE' | 'QR_TEXT' | 'OCR_TEXT';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'UNKNOWN';

export interface AiIndicator {
    title: string;
    description: string;
}

export interface AiAnalysisRequest {
    contentType: ContentType;
    content: string;
    localRiskScore: number;
    localRiskLevel: string;
    localIndicators: string[];
}

export interface AiAnalysisResponse {
    riskLevel: RiskLevel;
    riskScore: number;
    summary: string;
    indicators: AiIndicator[];
    recommendation: string;
    confidence: number;
    aiSource: 'gemini' | 'fallback';
}

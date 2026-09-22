export type ContentType = 'URL' | 'MESSAGE' | 'QR_TEXT' | 'OCR_TEXT';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'UNKNOWN';

export type ThreatCategory =
    | "PHISHING"
    | "PAYMENT_SCAM"
    | "FAKE_JOB"
    | "FAKE_INVESTMENT"
    | "LOTTERY_SCAM"
    | "IMPERSONATION"
    | "CREDENTIAL_THEFT"
    | "SUSPICIOUS_URL"
    | "QR_SCAM"
    | "SOCIAL_ENGINEERING"
    | "UNKNOWN";

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
    threatCategory: ThreatCategory;
}

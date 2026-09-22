import { Request, Response, NextFunction } from 'express';
import { AiAnalysisRequest } from '../models/aiModels';

export const validateAiRequest = (req: Request, res: Response, next: NextFunction) => {
    const body = req.body as AiAnalysisRequest;

    if (!body || !body.contentType || !body.content) {
        return res.status(400).json({ error: 'Missing required fields: contentType and content' });
    }

    if (typeof body.content !== 'string' || body.content.length > 2000) {
        return res.status(400).json({ error: 'Invalid content: must be a string under 2000 characters.' });
    }

    const validTypes = ['URL', 'MESSAGE', 'QR_TEXT', 'OCR_TEXT'];
    if (!validTypes.includes(body.contentType)) {
        return res.status(400).json({ error: 'Invalid contentType.' });
    }

    if (body.localRiskScore !== undefined && (typeof body.localRiskScore !== 'number' || body.localRiskScore < 0 || body.localRiskScore > 100)) {
        return res.status(400).json({ error: 'Invalid localRiskScore: must be between 0 and 100.' });
    }

    next();
};

import { Router, Request, Response } from 'express';
import rateLimit from 'express-rate-limit';
import { validateAiRequest } from '../middleware/validation';
import { verifyFirebaseToken } from '../middleware/auth';
import { AiService } from '../services/aiService';

const router = Router();
const aiService = new AiService();

const aiRateLimiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 20, // limit each IP to 20 requests per windowMs
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests from this IP, please try again after a minute.' }
});

router.post('/analyze', verifyFirebaseToken, aiRateLimiter, validateAiRequest, async (req: Request, res: Response) => {
    try {
        const result = await aiService.analyzeContent(req.body);
        return res.status(200).json(result);
    } catch (error: any) {
        return res.status(500).json({ error: error.message || 'Internal server error during AI analysis.' });
    }
});

export default router;

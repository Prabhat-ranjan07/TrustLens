import { Router, Request, Response } from 'express';
import { validateAiRequest } from '../middleware/validation';
import { verifyFirebaseToken } from '../middleware/auth';
import { AiService } from '../services/aiService';

const router = Router();
const aiService = new AiService();

router.post('/analyze', verifyFirebaseToken, validateAiRequest, async (req: Request, res: Response) => {
    try {
        const result = await aiService.analyzeContent(req.body);
        return res.status(200).json(result);
    } catch (error: any) {
        return res.status(500).json({ error: error.message || 'Internal server error during AI analysis.' });
    }
});

export default router;

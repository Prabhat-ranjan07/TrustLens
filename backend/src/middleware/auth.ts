import { Request, Response, NextFunction } from 'express';
import * as admin from 'firebase-admin';

export const verifyFirebaseToken = async (req: Request, res: Response, next: NextFunction) => {
    const authHeader = req.headers.authorization;
    const isProduction = process.env.NODE_ENV === 'production';

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        if (!isProduction && authHeader === 'Bearer dev-bypass-token') {
            return next();
        }
        return res.status(401).json({ error: 'Unauthorized: Missing or invalid Authorization header.' });
    }

    const token = authHeader.split('Bearer ')[1];
    if (!isProduction && token === 'dev-bypass-token') {
        return next();
    }

    if (isProduction && token === 'dev-bypass-token') {
        return res.status(401).json({ error: 'Unauthorized: Development bypass token is not allowed in production.' });
    }

    if (admin.apps.length === 0) {
        return res.status(503).json({ error: 'Authentication service unavailable.' });
    }

    try {
        const decodedToken = await admin.auth().verifyIdToken(token);
        (req as any).user = decodedToken;
        return next();
    } catch (error) {
        return res.status(401).json({ error: 'Unauthorized: Failed to verify Firebase token.' });
    }
};

import { Request, Response, NextFunction } from 'express';
import * as admin from 'firebase-admin';

export const verifyFirebaseToken = async (req: Request, res: Response, next: NextFunction) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        // For local development / phase 5 flexibility without forcing service account setup immediately:
        // Allow unauthenticated requests if header is explicitly 'Bearer dev-bypass-token'
        if (authHeader === 'Bearer dev-bypass-token') {
            return next();
        }
        return res.status(401).json({ error: 'Unauthorized: Missing or invalid Authorization header.' });
    }

    const token = authHeader.split('Bearer ')[1];
    if (token === 'dev-bypass-token') {
        return next();
    }

    try {
        if (admin.apps.length > 0) {
            const decodedToken = await admin.auth().verifyIdToken(token);
            (req as any).user = decodedToken;
        }
        next();
    } catch (error) {
        return res.status(401).json({ error: 'Unauthorized: Failed to verify Firebase token.' });
    }
};

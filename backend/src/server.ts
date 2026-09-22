import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import * as admin from 'firebase-admin';
import aiRouter from './routes/aiAnalysis';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;
const isProduction = process.env.NODE_ENV === 'production';

// Environment-aware CORS configuration
const corsOptions: cors.CorsOptions = {
    origin: (origin, callback) => {
        const allowedOrigin = process.env.CORS_ORIGIN;
        if (isProduction) {
            if (!allowedOrigin) {
                return callback(new Error('CORS configuration error: CORS_ORIGIN is required in production.'));
            }
            const allowedOrigins = allowedOrigin.split(',').map(o => o.trim());
            if (!origin || allowedOrigins.includes(origin)) {
                return callback(null, true);
            } else {
                return callback(new Error('Not allowed by CORS in production.'));
            }
        } else {
            // Development: allow requests with no origin (like mobile native apps / emulators) or specified origin
            if (!origin || !allowedOrigin || allowedOrigin === '*' || origin === allowedOrigin || origin.startsWith('http://localhost') || origin.startsWith('http://10.0.2.2')) {
                return callback(null, true);
            } else {
                return callback(null, true); // Permissive in dev
            }
        }
    }
};

app.use(cors(corsOptions));
app.use(express.json({ limit: '50kb' }));

// Initialize Firebase Admin if service account available
try {
    if (process.env.FIREBASE_SERVICE_ACCOUNT_PATH) {
        admin.initializeApp({
            credential: admin.credential.cert(require(process.env.FIREBASE_SERVICE_ACCOUNT_PATH))
        });
    } else {
        admin.initializeApp();
    }
} catch (e) {
    console.log('Firebase Admin initialized with default settings or mock.');
}

app.use('/api/ai', aiRouter);

app.get('/health', (req, res) => {
    res.status(200).json({
        status: 'ok',
        service: 'TrustLens AI Backend'
    });
});

app.listen(PORT, () => {
    console.log(`TrustLens secure backend running on port ${PORT} [Env: ${process.env.NODE_ENV || 'development'}]`);
    console.log(`Gemini Model: ${process.env.GEMINI_MODEL || 'gemini-3.6-flash'}`);
    console.log(`Firebase Admin: ${admin.apps.length > 0 ? 'Configured' : 'Mock/Unconfigured'}`);
});

export default app;

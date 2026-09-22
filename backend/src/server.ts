import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import * as admin from 'firebase-admin';
import aiRouter from './routes/aiAnalysis';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
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
    const firebaseConfigured = admin.apps.length > 0;
    const aiConfigured = !!process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== 'YOUR_SERVER_SIDE_GEMINI_API_KEY';
    res.status(200).json({
        status: 'ok',
        service: 'TrustLens AI Backend',
        firebase: firebaseConfigured ? 'configured' : 'not configured',
        ai: aiConfigured ? 'configured' : 'not configured'
    });
});

app.listen(PORT, () => {
    console.log(`TrustLens secure backend running on port ${PORT}`);
    console.log(`Gemini Model: ${process.env.GEMINI_MODEL || 'gemini-3.6-flash'}`);
    console.log(`Firebase Admin: ${admin.apps.length > 0 ? 'Configured' : 'Mock/Unconfigured'}`);
});

export default app;

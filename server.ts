import express from 'express';
import path from 'path';
import { createServer as createViteServer } from 'vite';

const app = express();
const PORT = 3000;

app.use(express.json());

// In-memory cache for fast search queries
const serverCache = new Map<string, { timestamp: number; data: any }>();
const CACHE_TTL_MS = 1000 * 60 * 30; // 30 mins

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: Date.now() });
});

// Resilient Open Food Facts search proxy
app.get('/api/off-search', async (req, res) => {
  const query = (req.query.q as string || '').trim();
  if (!query || query.length < 2) {
    return res.json({ products: [], source: 'empty' });
  }

  const cacheKey = query.toLowerCase();
  const cached = serverCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    return res.json({ products: cached.data, fromServerCache: true });
  }

  const endpoints = [
    `https://in.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(query)}&search_simple=1&action=process&json=1&page_size=25&fields=code,product_name,brands,nutriments,image_front_small_url,categories_tags`,
    `https://world.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(query)}&search_simple=1&action=process&json=1&page_size=25&fields=code,product_name,brands,nutriments,image_front_small_url,categories_tags`,
  ];

  let productsFound: any[] = [];
  let successful = false;

  for (const url of endpoints) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 4500);

      const response = await fetch(url, {
        headers: {
          'User-Agent': 'IndianNutritionWeightTracker/1.0 (contact: support@nutritionapp.local)',
          Accept: 'application/json',
        },
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (response.ok) {
        const data = await response.json();
        if (data.products && Array.isArray(data.products) && data.products.length > 0) {
          productsFound = data.products;
          successful = true;
          break;
        }
      }
    } catch (err: any) {
      console.warn(`Fetch to ${url} failed in proxy:`, err.message || err);
    }
  }

  if (successful && productsFound.length > 0) {
    serverCache.set(cacheKey, { timestamp: Date.now(), data: productsFound });
  }

  return res.json({
    products: productsFound,
    success: successful,
  });
});

async function startServer() {
  // Vite middleware for development
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();

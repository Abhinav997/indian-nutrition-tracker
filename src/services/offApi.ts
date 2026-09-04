import { FoodMaster, OffProductCache } from '../types';
import { INDIAN_PACKAGED_FOODS } from '../data/packagedFoods';

export interface OffSearchResult {
  foods: FoodMaster[];
  fromCache: boolean;
  error?: string;
}

function parseOffProduct(p: any): FoodMaster | null {
  if (!p || !p.product_name) return null;
  const nutriments = p.nutriments || {};
  const kcal = Math.round(
    Number(
      nutriments['energy-kcal_100g'] ??
        nutriments['energy-kcal'] ??
        nutriments.energy_kcal_100g ??
        (nutriments.energy_100g ? nutriments.energy_100g / 4.184 : 0)
    )
  ) || 0;
  const protein = Number((nutriments.proteins_100g ?? nutriments.protein_100g ?? 0).toFixed(1));
  const carbs = Number((nutriments.carbohydrates_100g ?? 0).toFixed(1));
  const fat = Number((nutriments.fat_100g ?? 0).toFixed(1));
  const fiber = Number((nutriments.fiber_100g ?? 0).toFixed(1));

  if (kcal === 0 && protein === 0 && carbs === 0 && fat === 0) {
    return null;
  }

  return {
    id: `off_${p.code || Math.random().toString(36).substring(7)}`,
    name: p.product_name,
    brand: p.brands || 'Packaged Product',
    source: 'OFF' as const,
    kcal_per_100g: kcal,
    protein_per_100g: protein,
    carbs_per_100g: carbs,
    fat_per_100g: fat,
    fiber_per_100g: fiber > 0 ? fiber : undefined,
    typical_serving_description: '100g packaged serving',
    typical_serving_grams: 100,
    barcode: p.code,
    image_url: p.image_front_small_url,
  };
}

/**
 * Searches Open Food Facts API with server-proxy, direct fetch, and local cache fallback
 */
export async function searchOpenFoodFacts(
  query: string,
  localCache: Record<string, OffProductCache> = {}
): Promise<OffSearchResult> {
  const trimmed = query.trim().toLowerCase();
  if (!trimmed || trimmed.length < 2) {
    return { foods: [], fromCache: false };
  }

  // Pre-seed matching items from curated Indian Packaged Food list
  const packagedMatches: FoodMaster[] = INDIAN_PACKAGED_FOODS.filter(
    (item) =>
      item.name.toLowerCase().includes(trimmed) ||
      (item.brand && item.brand.toLowerCase().includes(trimmed)) ||
      (item.category && item.category.toLowerCase().includes(trimmed))
  );

  // Check matching products in user's previous local cache
  const cachedMatches: FoodMaster[] = Object.values(localCache)
    .filter(
      (item) =>
        item.product_name.toLowerCase().includes(trimmed) ||
        (item.brand && item.brand.toLowerCase().includes(trimmed))
    )
    .map((cached) => ({
      id: `off_cache_${cached.barcode || cached.product_name.replace(/\s+/g, '_')}`,
      name: cached.product_name,
      brand: cached.brand,
      source: 'OFF',
      kcal_per_100g: cached.kcal_per_100g,
      protein_per_100g: cached.protein_per_100g,
      carbs_per_100g: cached.carbs_per_100g,
      fat_per_100g: cached.fat_per_100g,
      typical_serving_description: 'Standard 100g portion',
      typical_serving_grams: 100,
      barcode: cached.barcode,
    }));

  const localFallbacks = [...packagedMatches, ...cachedMatches];

  // 1. First attempt: Server-Side API Proxy (/api/off-search)
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const proxyRes = await fetch(`/api/off-search?q=${encodeURIComponent(query)}`, {
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (proxyRes.ok) {
      const data = await proxyRes.json();
      if (data.products && Array.isArray(data.products) && data.products.length > 0) {
        const parsed: FoodMaster[] = [];
        for (const p of data.products) {
          const item = parseOffProduct(p);
          if (item) parsed.push(item);
        }

        // Merge with any unique curated matches
        const resultMap = new Map<string, FoodMaster>();
        localFallbacks.forEach((f) => resultMap.set(f.name.toLowerCase(), f));
        parsed.forEach((f) => resultMap.set(f.name.toLowerCase(), f));

        return { foods: Array.from(resultMap.values()), fromCache: false };
      }
    }
  } catch {
    // Continue to direct fetch attempt
  }

  // 2. Second attempt: Direct Client Fetch to Open Food Facts (WITHOUT forbidden User-Agent header)
  const directUrls = [
    `https://in.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(query)}&search_simple=1&action=process&json=1&page_size=20&fields=code,product_name,brands,nutriments,image_front_small_url,categories_tags`,
    `https://world.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(query)}&search_simple=1&action=process&json=1&page_size=20&fields=code,product_name,brands,nutriments,image_front_small_url,categories_tags`,
  ];

  for (const searchUrl of directUrls) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 4000);

      const response = await fetch(searchUrl, {
        headers: {
          Accept: 'application/json',
        },
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (response.ok) {
        const data = await response.json();
        const products = data.products || [];

        const fetchedFoods: FoodMaster[] = [];
        for (const p of products) {
          const item = parseOffProduct(p);
          if (item) fetchedFoods.push(item);
        }

        if (fetchedFoods.length > 0) {
          const resultMap = new Map<string, FoodMaster>();
          localFallbacks.forEach((f) => resultMap.set(f.name.toLowerCase(), f));
          fetchedFoods.forEach((f) => resultMap.set(f.name.toLowerCase(), f));

          return { foods: Array.from(resultMap.values()), fromCache: false };
        }
      }
    } catch {
      // Try next endpoint
    }
  }

  // 3. Fallback: Return curated packaged items & cache without intrusive failure alert
  if (localFallbacks.length > 0) {
    const resultMap = new Map<string, FoodMaster>();
    localFallbacks.forEach((f) => resultMap.set(f.name.toLowerCase(), f));
    return {
      foods: Array.from(resultMap.values()),
      fromCache: true,
    };
  }

  return {
    foods: [],
    fromCache: true,
  };
}


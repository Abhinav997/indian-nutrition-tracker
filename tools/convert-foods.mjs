#!/usr/bin/env node
/**
 * One-off converter: extracts the food arrays from the web app's TypeScript
 * data files (from commit 2d4a7e3) and emits camelCase JSON for the native
 * app assets. Ids and numeric values are preserved byte-for-byte.
 *
 * Usage: node tools/convert-foods.mjs
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const outDir = join(root, 'app', 'src', 'main', 'assets', 'data');
mkdirSync(outDir, { recursive: true });

function extractArray(sourcePath, constName) {
  const text = readFileSync(join(root, sourcePath), 'utf8');
  const start = text.indexOf(`= [`);
  const end = text.lastIndexOf('];');
  if (start < 0 || end < 0) throw new Error(`Cannot find array in ${sourcePath}`);
  const literal = text.slice(start + 2, end + 1);
  // Evaluate the pure JS object literal (the TS file is source-compatible JS here)
  const sandbox = { console };
  const value = vm.runInNewContext(`(${literal})`, sandbox);
  if (!Array.isArray(value)) throw new Error(`${constName} is not an array`);
  return value;
}

function toJson(item) {
  const out = {
    id: item.id,
    name: item.name,
    source: item.source,
    kcalPer100g: item.kcal_per_100g,
    proteinPer100g: item.protein_per_100g,
    carbsPer100g: item.carbs_per_100g,
    fatPer100g: item.fat_per_100g,
  };
  if (item.fiber_per_100g !== undefined) out.fiberPer100g = item.fiber_per_100g;
  if (item.typical_serving_description !== undefined) out.typicalServingDescription = item.typical_serving_description;
  if (item.typical_serving_grams !== undefined) out.typicalServingGrams = item.typical_serving_grams;
  if (item.brand !== undefined) out.brand = item.brand;
  if (item.category !== undefined) out.category = item.category;
  if (item.barcode !== undefined) out.barcode = item.barcode;
  if (item.image_url !== undefined) out.imageUrl = item.image_url;
  return out;
}

const nin = extractArray('src/data/nin_ifct_data.ts', 'NIN_IFCT_FOODS').map(toJson);
const packaged = extractArray('src/data/packagedFoods.ts', 'INDIAN_PACKAGED_FOODS').map(toJson);

writeFileSync(join(outDir, 'nin_ifct.json'), JSON.stringify(nin, null, 2) + '\n');
writeFileSync(join(outDir, 'packaged_foods.json'), JSON.stringify(packaged, null, 2) + '\n');

console.log(`nin_ifct.json   : ${nin.length} items`);
console.log(`packaged_foods.json: ${packaged.length} items`);
console.log(`total: ${nin.length + packaged.length}`);

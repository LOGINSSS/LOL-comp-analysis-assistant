# LOLCAA Frontend (Rebuild)

Vue 3 draft-pick UI rebuild for League champion select, with dark esports styling and motion-heavy interactions.

## Stack

- Vue 3 + TypeScript + Vite
- Tailwind CSS
- Motion for Vue (`@vueuse/motion`)
- Axios

## Key Features Implemented

- Three-column draft layout (blue side / hero pool / red side)
- Top header with 10 ban slots (5 left, 5 right)
- Role filter (`ALL/TOP/JG/MID/ADC/SUP`) and name search on hero pool
- Role filtering rule: `primary_role` OR `secondary_role`
- Hero pool ordered by `id` ascending
- Red-side `FINAL PICK` action: lock and submit flow
- Result panel with team analysis, win-rate bar, recommendations, counters

## Project Structure

- `src/App.vue`: orchestration and state flow
- `src/components/`: `DraftHeader`, `TeamPanel`, `HeroPool`, `ChampionCard`, `ChampionPreview`, `DraftActionBar`, `AnalysisResult`
- `src/data/mockChampions.ts`: mock champion dataset (30 entries)
- `src/types/draft.ts`: draft domain interfaces
- `src/api/draft.ts`: submit adapter and fallback result
- `src/styles/index.css`: Tailwind entry and custom theme classes

## Run

```bash
cd "D:\ASTUDY\TheRoadOfGold\project\LOL comp analysis assistant\frontend"
npm install
npm run dev
```

## Build

```bash
npm run build
npm run preview
```

## API Notes

- Default base URL: `/api`
- Submit endpoint used by UI: `POST /last-pick/analyze`
- Override with env variable: `VITE_API_BASE_URL`


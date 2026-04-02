<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

import { analyzeLastPick } from '@/api/lastPick';
import { championPool, roleNameMap, roleOrder, type RoleKey } from '@/data/champions';
import type { ChampionOption, DraftAnalysisResponse, LastPickAnalysisRequest } from '@/types/lastPick';

type TeamMap = Record<RoleKey, number | null>;

const championMap = new Map(championPool.map((champion) => [champion.id, champion]));
const loading = ref(false);
const result = ref<DraftAnalysisResponse | null>(null);
const activePanels = ref(['matchup']);
const activeBanSlot = ref(0);
const activePoolRole = ref<RoleKey>('TOP');
const poolKeyword = ref('');
const banSlots = ref<Array<number | null>>(Array.from({ length: 10 }, () => null));
const activePickTeam = ref<'ally' | 'enemy'>('ally');
const activePickRole = ref<RoleKey>('TOP');
const pickPoolRole = ref<RoleKey>('TOP');
const pickKeyword = ref('');

const form = reactive({
  requiredRole: 'MID' as RoleKey
});

const allyByRole = reactive<TeamMap>({
  TOP: null,
  JUNGLE: null,
  MID: null,
  ADC: null,
  SUP: null
});

const enemyByRole = reactive<TeamMap>({
  TOP: null,
  JUNGLE: null,
  MID: null,
  ADC: null,
  SUP: null
});

watch(
  () => form.requiredRole,
  (role) => {
    allyByRole[role] = null;
    if (activePickTeam.value === 'ally' && activePickRole.value === role) {
      const fallback = roleOrder.find((item) => item !== role) ?? 'TOP';
      activePickRole.value = fallback;
      pickPoolRole.value = fallback;
    }
  },
  { immediate: true }
);

const banChampionIds = computed(() => banSlots.value.filter((id): id is number => typeof id === 'number'));
const filteredPool = computed(() => {
  const keyword = poolKeyword.value.trim().toLowerCase();
  return championPool.filter((champion) => {
    const inRole = champion.primaryRole === activePoolRole.value || champion.secondaryRole === activePoolRole.value;
    if (!inRole) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    return champion.name.toLowerCase().includes(keyword) || String(champion.id).includes(keyword);
  });
});

const currentPickChampionId = computed(() => {
  const source = activePickTeam.value === 'ally' ? allyByRole : enemyByRole;
  return source[activePickRole.value];
});

const filteredPickPool = computed(() => {
  const keyword = pickKeyword.value.trim().toLowerCase();
  return championPool.filter((champion) => {
    const inRole = champion.primaryRole === pickPoolRole.value || champion.secondaryRole === pickPoolRole.value;
    if (!inRole) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    return champion.name.toLowerCase().includes(keyword) || String(champion.id).includes(keyword);
  });
});

const allyPickedChampions = computed(() =>
  roleOrder
    .filter((role) => role !== form.requiredRole)
    .map((role) => allyByRole[role])
    .filter((id): id is number => typeof id === 'number')
);

const enemyPickedChampions = computed(() =>
  roleOrder.map((role) => enemyByRole[role]).filter((id): id is number => typeof id === 'number')
);

const selectedBans = computed(() => heroCards(banChampionIds.value));
const selectedAllies = computed(() => heroCards(allyPickedChampions.value));
const selectedEnemies = computed(() => heroCards(enemyPickedChampions.value));

const conflictNames = computed(() => {
  const bag = [...banChampionIds.value, ...allyPickedChampions.value, ...enemyPickedChampions.value];
  const seen = new Set<number>();
  const repeated = new Set<number>();
  bag.forEach((id) => {
    if (seen.has(id)) {
      repeated.add(id);
    }
    seen.add(id);
  });
  return [...repeated].map((id) => championLabel(id));
});

const draftStatus = computed(
  () => `ban ${banChampionIds.value.length}/10 · 己方 ${allyPickedChampions.value.length}/4 · 敌方 ${enemyPickedChampions.value.length}/5`
);

const summaryScore = computed(() => formatScore(result.value?.finalScore));
const metrics = computed(() => [
  { label: '对线克制', value: result.value?.matchupScore ?? 0, tone: 'normal' },
  { label: '双人协同', value: result.value?.synergyScore ?? 0, tone: 'normal' },
  { label: '整体阵容', value: result.value?.teamSynergyScore ?? 0, tone: 'normal' },
  { label: '己方压制', value: result.value?.allyDestructiveScore ?? 0, tone: 'normal' },
  { label: '敌方威胁', value: result.value?.enemyDestructiveScore ?? 0, tone: 'danger' }
]);

function fillSampleDraft() {
  form.requiredRole = 'MID';
  banSlots.value = [84, 64, 22, 67, 25, 51, 157, 58, 12, 517];
  activeBanSlot.value = 0;
  activePoolRole.value = 'TOP';
  poolKeyword.value = '';
  activePickTeam.value = 'ally';
  activePickRole.value = 'TOP';
  pickPoolRole.value = 'TOP';
  pickKeyword.value = '';
  allyByRole.TOP = 266;
  allyByRole.JUNGLE = 79;
  allyByRole.MID = null;
  allyByRole.ADC = 81;
  allyByRole.SUP = 412;

  enemyByRole.TOP = 54;
  enemyByRole.JUNGLE = 72;
  enemyByRole.MID = 238;
  enemyByRole.ADC = 110;
  enemyByRole.SUP = 111;
}

function clearDraft() {
  form.requiredRole = 'MID';
  banSlots.value = Array.from({ length: 10 }, () => null);
  activeBanSlot.value = 0;
  activePoolRole.value = 'TOP';
  poolKeyword.value = '';
  activePickTeam.value = 'ally';
  activePickRole.value = 'TOP';
  pickPoolRole.value = 'TOP';
  pickKeyword.value = '';
  roleOrder.forEach((role) => {
    allyByRole[role] = null;
    enemyByRole[role] = null;
  });
  result.value = null;
}

function slotChampion(index: number) {
  const id = banSlots.value[index];
  if (typeof id !== 'number') {
    return null;
  }
  return championMap.get(id) ?? null;
}

function championLabel(id: number) {
  const champion = championMap.get(id);
  return champion ? champion.name : `未知英雄 #${id}`;
}

function heroCards(ids: number[]) {
  return ids
    .map((id) => championMap.get(id))
    .filter((champion): champion is ChampionOption => Boolean(champion))
    .map((champion) => ({
      id: champion.id,
      name: champion.name,
      avatar: championAvatar(champion.name)
    }));
}

function championAvatar(name: string) {
  const alias: Record<string, string> = {
    'Lee Sin': 'LeeSin'
  };
  const slug = (alias[name] ?? name).replace(/[\s'.]/g, '');
  return `https://ddragon.leagueoflegends.com/cdn/14.24.1/img/champion/${slug}.png`;
}

function isBanDisabled(championId: number) {
  return banSlots.value.some((id, index) => index !== activeBanSlot.value && id === championId);
}

function chooseBanChampion(championId: number) {
  if (isBanDisabled(championId)) {
    ElMessage.warning('该英雄已占用其他 ban 位');
    return;
  }
  banSlots.value[activeBanSlot.value] = championId;
}

function clearBanSlot(index: number) {
  banSlots.value[index] = null;
}

function slotPickChampion(team: 'ally' | 'enemy', role: RoleKey) {
  const source = team === 'ally' ? allyByRole : enemyByRole;
  const id = source[role];
  if (typeof id !== 'number') {
    return null;
  }
  return championMap.get(id) ?? null;
}

function isAllyLockedRole(role: RoleKey) {
  return role === form.requiredRole;
}

function activatePickSlot(team: 'ally' | 'enemy', role: RoleKey) {
  if (team === 'ally' && isAllyLockedRole(role)) {
    return;
  }
  activePickTeam.value = team;
  activePickRole.value = role;
  pickPoolRole.value = role;
}

function isPickPoolDisabled(championId: number) {
  if (banChampionIds.value.includes(championId)) {
    return true;
  }

  return roleOrder.some((role) => {
    const allyId = allyByRole[role];
    const enemyId = enemyByRole[role];
    const sameActiveAlly = activePickTeam.value === 'ally' && activePickRole.value === role;
    const sameActiveEnemy = activePickTeam.value === 'enemy' && activePickRole.value === role;

    if (allyId === championId && !sameActiveAlly) {
      return true;
    }
    if (enemyId === championId && !sameActiveEnemy) {
      return true;
    }
    return false;
  });
}

function choosePickChampion(championId: number) {
  if (activePickTeam.value === 'ally' && isAllyLockedRole(activePickRole.value)) {
    return;
  }
  if (isPickPoolDisabled(championId)) {
    ElMessage.warning('该英雄已被禁用或已在阵容中使用');
    return;
  }

  const source = activePickTeam.value === 'ally' ? allyByRole : enemyByRole;
  source[activePickRole.value] = championId;
}

function clearPickSlot(team: 'ally' | 'enemy', role: RoleKey) {
  if (team === 'ally' && isAllyLockedRole(role)) {
    return;
  }
  const source = team === 'ally' ? allyByRole : enemyByRole;
  source[role] = null;
}

function formatScore(score?: number) {
  if (typeof score !== 'number') {
    return '--';
  }
  return score.toFixed(1);
}

function prettyDetail(detail?: string) {
  if (!detail) {
    return '暂无分析详情';
  }
  try {
    return JSON.stringify(JSON.parse(detail), null, 2);
  } catch {
    return detail;
  }
}

async function submitAnalysis() {
  if (allyPickedChampions.value.length !== 4) {
    ElMessage.error('己方必须选满 4 个位置，最后一选对应的位置保持空位。');
    return;
  }
  if (enemyPickedChampions.value.length !== 5) {
    ElMessage.error('敌方必须按位置选满 5 个英雄。');
    return;
  }
  if (conflictNames.value.length > 0) {
    ElMessage.error(`存在重复英雄：${conflictNames.value.join('、')}`);
    return;
  }

  const payload: LastPickAnalysisRequest = {
    bannedChampions: [...new Set(banChampionIds.value)],
    allyPickedChampions: allyPickedChampions.value,
    enemyPickedChampions: enemyPickedChampions.value,
    requiredRole: form.requiredRole
  };

  loading.value = true;
  try {
    result.value = await analyzeLastPick(payload);
    ElMessage.success('分析完成');
  } catch (error) {
    const message = error instanceof Error ? error.message : '请求失败，请检查后端是否启动';
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="app-shell">
    <header class="page-header">
      <div class="brand-group">
        <div class="brand-mark">LOLCAA</div>
        <div>
          <h1>红色方最后一选</h1>
          <p>基于阵容数据、对线关系与团队协同的最后一选分析台</p>
        </div>
      </div>
      <div class="header-strip">
        <span>规则引擎</span>
        <span>暗色主题</span>
        <span>Vue 3 + Element Plus</span>
      </div>
    </header>

    <main class="page-grid">
      <section class="left-column">
        <el-card class="panel-card input-card" shadow="never">
          <template #header>
            <div class="card-head">
              <div>
                <h2 class="section-title">输入阵容</h2>
                <p class="soft-text">建议按 10 ban / 4 ally / 5 enemy 的剧本填写。</p>
              </div>
              <span class="status-pill">{{ draftStatus }}</span>
            </div>
          </template>

          <el-alert
            v-if="conflictNames.length > 0"
            class="warning-block"
            title="当前存在重复英雄，建议先清理后再分析"
            :description="conflictNames.join('、')"
            type="warning"
            :closable="false"
            show-icon
          />

          <div class="ban-block">
            <div class="ban-head">
              <label>Ban 位</label>
              <span>点击上方格子，然后从下方英雄池选择</span>
            </div>

            <div class="ban-slot-grid">
              <button
                v-for="index in 10"
                :key="`ban-slot-${index - 1}`"
                type="button"
                class="ban-slot"
                :class="{ active: activeBanSlot === index - 1, filled: Boolean(slotChampion(index - 1)) }"
                @click="activeBanSlot = index - 1"
              >
                <img
                  v-if="slotChampion(index - 1)"
                  :src="championAvatar(slotChampion(index - 1)!.name)"
                  :alt="slotChampion(index - 1)!.name"
                />
                <span v-else>BAN {{ index }}</span>
                <i v-if="slotChampion(index - 1)" class="clear-slot" @click.stop="clearBanSlot(index - 1)">x</i>
              </button>
            </div>
          </div>

          <div class="pool-block">
            <div class="pool-tool-row">
              <el-radio-group v-model="activePoolRole" class="pool-role-tabs">
                <el-radio-button v-for="role in roleOrder" :key="`pool-${role}`" :label="role">
                  {{ roleNameMap[role] }}
                </el-radio-button>
              </el-radio-group>
              <el-input v-model="poolKeyword" clearable class="pool-search" placeholder="搜索英雄名或 ID" />
            </div>

            <div class="pool-grid">
              <button
                v-for="champion in filteredPool"
                :key="`pool-card-${champion.id}`"
                type="button"
                class="pool-card"
                :class="{ disabled: isBanDisabled(champion.id), selected: banSlots[activeBanSlot] === champion.id }"
                :disabled="isBanDisabled(champion.id)"
                @click="chooseBanChampion(champion.id)"
              >
                <img :src="championAvatar(champion.name)" :alt="champion.name" />
                <span>{{ champion.name }}</span>
              </button>
            </div>
          </div>

          <div class="draft-block">
            <div class="draft-head">
              <label>阵容位</label>
              <span>点击任意位置，再从下方英雄池点选</span>
            </div>

            <div class="team-slot-layout">
              <div class="team-column ally-column">
                <p class="team-title">蓝色方（己方）</p>
                <button
                  v-for="role in roleOrder"
                  :key="`ally-pick-${role}`"
                  type="button"
                  class="pick-slot"
                  :class="{
                    active: activePickTeam === 'ally' && activePickRole === role,
                    locked: isAllyLockedRole(role),
                    filled: Boolean(slotPickChampion('ally', role))
                  }"
                  @click="activatePickSlot('ally', role)"
                >
                  <span class="pick-role">{{ roleNameMap[role] }}</span>
                  <template v-if="slotPickChampion('ally', role)">
                    <img :src="championAvatar(slotPickChampion('ally', role)!.name)" :alt="slotPickChampion('ally', role)!.name" />
                    <span class="pick-name">{{ slotPickChampion('ally', role)!.name }}</span>
                  </template>
                  <span v-else class="pick-placeholder">{{ role === form.requiredRole ? '最后一选保留位' : '点击选择英雄' }}</span>
                  <i
                    v-if="slotPickChampion('ally', role) && !isAllyLockedRole(role)"
                    class="clear-slot"
                    @click.stop="clearPickSlot('ally', role)"
                  >x</i>
                </button>
              </div>

              <div class="team-column enemy-column">
                <p class="team-title">红色方（敌方）</p>
                <button
                  v-for="role in roleOrder"
                  :key="`enemy-pick-${role}`"
                  type="button"
                  class="pick-slot"
                  :class="{ active: activePickTeam === 'enemy' && activePickRole === role, filled: Boolean(slotPickChampion('enemy', role)) }"
                  @click="activatePickSlot('enemy', role)"
                >
                  <span class="pick-role">{{ roleNameMap[role] }}</span>
                  <template v-if="slotPickChampion('enemy', role)">
                    <img :src="championAvatar(slotPickChampion('enemy', role)!.name)" :alt="slotPickChampion('enemy', role)!.name" />
                    <span class="pick-name">{{ slotPickChampion('enemy', role)!.name }}</span>
                  </template>
                  <span v-else class="pick-placeholder">点击选择英雄</span>
                  <i v-if="slotPickChampion('enemy', role)" class="clear-slot" @click.stop="clearPickSlot('enemy', role)">x</i>
                </button>
              </div>
            </div>
          </div>

          <div class="pool-block pick-pool">
            <div class="pool-tool-row">
              <el-radio-group v-model="pickPoolRole" class="pool-role-tabs">
                <el-radio-button v-for="role in roleOrder" :key="`pick-pool-${role}`" :label="role">
                  {{ roleNameMap[role] }}
                </el-radio-button>
              </el-radio-group>
              <el-input v-model="pickKeyword" clearable class="pool-search" placeholder="搜索英雄名或 ID" />
            </div>

            <div class="pool-selection-tip">当前选择：{{ activePickTeam === 'ally' ? '己方' : '敌方' }} · {{ roleNameMap[activePickRole] }}</div>

            <div class="pool-grid">
              <button
                v-for="champion in filteredPickPool"
                :key="`pick-card-${champion.id}`"
                type="button"
                class="pool-card"
                :class="{ disabled: isPickPoolDisabled(champion.id), selected: currentPickChampionId === champion.id }"
                :disabled="isPickPoolDisabled(champion.id)"
                @click="choosePickChampion(champion.id)"
              >
                <img :src="championAvatar(champion.name)" :alt="champion.name" />
                <span>{{ champion.name }}</span>
              </button>
            </div>
          </div>

          <div class="field-block role-block">
            <label>需求位置</label>
            <el-radio-group v-model="form.requiredRole" size="large" class="role-group">
              <el-radio-button v-for="role in roleOrder" :key="role" :label="role">{{ roleNameMap[role] }}</el-radio-button>
            </el-radio-group>
          </div>

          <div class="preview-wrap">
            <div class="preview-block">
              <p class="preview-title">已 ban 英雄</p>
              <div class="hero-grid">
                <div v-for="hero in selectedBans" :key="`ban-card-${hero.id}`" class="hero-card">
                  <img :src="hero.avatar" :alt="hero.name" />
                  <span>{{ hero.name }}</span>
                </div>
              </div>
            </div>

            <div class="preview-block">
              <p class="preview-title">己方已选英雄</p>
              <div class="hero-grid">
                <div v-for="hero in selectedAllies" :key="`ally-card-${hero.id}`" class="hero-card ally-card">
                  <img :src="hero.avatar" :alt="hero.name" />
                  <span>{{ hero.name }}</span>
                </div>
              </div>
            </div>

            <div class="preview-block">
              <p class="preview-title">敌方已选英雄</p>
              <div class="hero-grid">
                <div v-for="hero in selectedEnemies" :key="`enemy-card-${hero.id}`" class="hero-card enemy-card">
                  <img :src="hero.avatar" :alt="hero.name" />
                  <span>{{ hero.name }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="button-row">
            <el-button type="primary" :loading="loading" @click="submitAnalysis">开始分析</el-button>
            <el-button plain @click="fillSampleDraft">填入示例</el-button>
            <el-button plain @click="clearDraft">清空</el-button>
          </div>
        </el-card>
      </section>

      <section class="right-column">
        <el-card class="panel-card summary-card" shadow="never">
          <template #header>
            <div class="card-head">
              <div>
                <h2 class="section-title">战术输出</h2>
                <p class="soft-text">右侧输出保持独立，便于后续挂接更多分析模块。</p>
              </div>
              <span class="status-pill accent-pill">实时分析</span>
            </div>
          </template>

          <div class="analysis-banner">
            <span class="side-blue">BLUE</span>
            <span class="side-ready">READY</span>
            <span class="side-red">RED</span>
          </div>

          <div class="summary-layout" v-if="result">
            <div class="summary-emblem">
              <span>Final</span>
              <strong>{{ summaryScore }}</strong>
            </div>
            <div class="summary-copy">
              <div class="summary-line">{{ result.recommendation }}</div>
              <div class="summary-meta">
                <span>胜率判断：{{ result.winProbability }}</span>
                <span>分析耗时：{{ result.analysisTime ?? '--' }} ms</span>
                <span>草稿编号：{{ result.draftId ?? '--' }}</span>
              </div>
            </div>
          </div>

          <div v-else class="empty-state">填写阵容并点击“开始分析”后，结果会显示在这里。</div>
        </el-card>

        <el-row :gutter="16" class="metric-grid">
          <el-col v-for="metric in metrics" :key="metric.label" :xs="24" :sm="12" :lg="8">
            <el-card class="panel-card metric-card" shadow="never">
              <div class="metric-head">
                <span>{{ metric.label }}</span>
                <strong>{{ formatScore(metric.value) }}</strong>
              </div>
              <el-progress
                :percentage="Math.min(100, Math.max(0, metric.value))"
                :stroke-width="10"
                :show-text="false"
                :status="metric.tone === 'danger' ? 'exception' : undefined"
              />
            </el-card>
          </el-col>
        </el-row>

        <el-card class="panel-card detail-card" shadow="never">
          <template #header>
            <div class="card-head">
              <div>
                <h2 class="section-title">分析详情</h2>
                <p class="soft-text">原始解释会保留给后续大模型二次解读。</p>
              </div>
            </div>
          </template>

          <el-collapse v-model="activePanels" accordion>
            <el-collapse-item name="matchup">
              <template #title>对线克制</template>
              <pre class="detail-pre">{{ prettyDetail(result?.matchupDetail) }}</pre>
            </el-collapse-item>
            <el-collapse-item name="synergy">
              <template #title>双人协同</template>
              <pre class="detail-pre">{{ prettyDetail(result?.synergyDetail) }}</pre>
            </el-collapse-item>
            <el-collapse-item name="team">
              <template #title>整体阵容</template>
              <pre class="detail-pre">{{ prettyDetail(result?.teamSynergyDetail) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss">
.app-shell {
  position: relative;
  z-index: 1;
  width: min(1760px, calc(100vw - 20px));
  margin: 0 auto 0 12px;
  padding: 24px 0 40px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
  padding: 18px 22px;
  border: 1px solid rgba(134, 156, 255, 0.14);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(9, 15, 28, 0.96), rgba(9, 15, 28, 0.82));
  backdrop-filter: blur(16px);
  box-shadow: 0 26px 48px rgba(0, 0, 0, 0.22);
}

.brand-group {
  display: flex;
  align-items: center;
  gap: 16px;

  h1 {
    margin: 0;
    font-size: 28px;
    letter-spacing: 0.02em;
  }

  p {
    margin: 6px 0 0;
    color: #92a0c2;
    font-size: 13px;
  }
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border: 1px solid rgba(143, 116, 255, 0.28);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(143, 116, 255, 0.2), rgba(57, 197, 255, 0.08));
  color: #f3f6ff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.header-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;

  span {
    padding: 8px 12px;
    border: 1px solid rgba(134, 156, 255, 0.14);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.04);
    color: #cbd4ef;
    font-size: 12px;
  }
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 440px;
  gap: 20px;
}

.left-column,
.right-column {
  min-width: 0;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.status-pill {
  flex-shrink: 0;
  padding: 8px 12px;
  border: 1px solid rgba(134, 156, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: #cbd4ef;
  font-size: 12px;
}

.accent-pill {
  border-color: rgba(143, 116, 255, 0.3);
  background: rgba(143, 116, 255, 0.12);
  color: #e8e1ff;
}

.input-card,
.summary-card,
.detail-card {
  margin-bottom: 18px;
}

.warning-block {
  margin-bottom: 16px;
}

.field-block {
  margin-bottom: 18px;

  label {
    display: block;
    margin-bottom: 10px;
    color: #dbe3ff;
    font-size: 13px;
    letter-spacing: 0.02em;
  }
}

.ban-block {
  margin-bottom: 18px;
  padding: 12px;
  border: 1px solid rgba(134, 156, 255, 0.14);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.02);
}

.ban-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;

  label {
    margin: 0;
    color: #dbe3ff;
    font-size: 13px;
    letter-spacing: 0.02em;
  }

  span {
    color: #8f9bc0;
    font-size: 12px;
  }
}

.ban-slot-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.ban-slot {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 64px;
  border: 1px solid rgba(134, 156, 255, 0.18);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: #97a4cb;
  cursor: pointer;
  transition: all 0.2s ease;

  img {
    width: 100%;
    height: 62px;
    object-fit: cover;
    border-radius: 9px;
  }

  span {
    font-size: 12px;
    letter-spacing: 0.04em;
  }
}

.ban-slot:hover {
  border-color: rgba(143, 116, 255, 0.45);
}

.ban-slot.active {
  border-color: rgba(143, 116, 255, 0.9);
  box-shadow: 0 0 0 1px rgba(143, 116, 255, 0.5) inset;
}

.ban-slot.filled {
  background: rgba(8, 13, 24, 0.85);
}

.clear-slot {
  position: absolute;
  top: 4px;
  right: 6px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 10px;
  font-style: normal;
  line-height: 16px;
}

.pool-block {
  margin-bottom: 18px;
  padding: 12px;
  border: 1px solid rgba(134, 156, 255, 0.14);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.02);
}

.pool-tool-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.pool-role-tabs {
  :deep(.el-radio-button__inner) {
    min-width: 52px;
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(134, 156, 255, 0.18);
    color: #cbd4ef;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: linear-gradient(180deg, rgba(143, 116, 255, 0.95), rgba(105, 84, 204, 0.95));
    border-color: rgba(143, 116, 255, 0.95);
    color: #fff;
  }
}

.pool-search {
  flex: 1;
  min-width: 220px;
}

.pool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(74px, 1fr));
  gap: 8px;
  max-height: 360px;
  overflow: auto;
}

.pool-card {
  display: grid;
  gap: 5px;
  justify-items: center;
  padding: 6px 4px;
  border: 1px solid rgba(134, 156, 255, 0.14);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.02);
  color: #ced6ef;
  cursor: pointer;

  img {
    width: 52px;
    height: 52px;
    border-radius: 8px;
    object-fit: cover;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }

  span {
    font-size: 11px;
    line-height: 1.2;
    text-align: center;
  }
}

.pool-card.selected {
  border-color: rgba(143, 116, 255, 0.85);
  box-shadow: 0 0 0 1px rgba(143, 116, 255, 0.5) inset;
}

.pool-card.disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.champion-select {
  width: 100%;
}

.compact-row {
  margin-top: 10px;
  min-height: 28px;
}

.slot-grid {
  display: grid;
  gap: 10px;
}

.slot-row {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.slot-label {
  color: #aab7dd;
  font-size: 12px;
}

.preview-wrap {
  display: grid;
  gap: 12px;
  margin-top: 6px;
}

.preview-block {
  padding: 10px;
  border: 1px solid rgba(134, 156, 255, 0.14);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.preview-title {
  margin: 0 0 10px;
  color: #dbe3ff;
  font-size: 12px;
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(76px, 1fr));
  gap: 8px;
}

.hero-card {
  display: grid;
  gap: 6px;
  justify-items: center;
  padding: 8px 6px;
  border: 1px solid rgba(134, 156, 255, 0.14);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.02);

  img {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    object-fit: cover;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }

  span {
    color: #ced6ef;
    font-size: 11px;
    text-align: center;
    line-height: 1.3;
  }
}

.ally-card {
  border-color: rgba(82, 208, 161, 0.26);
}

.enemy-card {
  border-color: rgba(245, 102, 102, 0.26);
}

.role-block {
  margin-top: 4px;
}

.role-group {
  width: 100%;

  :deep(.el-radio-button__inner) {
    min-width: 60px;
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(134, 156, 255, 0.18);
    color: #cbd4ef;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: linear-gradient(180deg, rgba(143, 116, 255, 0.95), rgba(105, 84, 204, 0.95));
    border-color: rgba(143, 116, 255, 0.95);
    color: #fff;
  }
}

.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.summary-layout {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.summary-emblem {
  display: grid;
  place-items: center;
  gap: 6px;
  min-height: 128px;
  border: 1px solid rgba(134, 156, 255, 0.18);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(143, 116, 255, 0.18), rgba(57, 197, 255, 0.08));

  span {
    color: #b7c3ee;
    font-size: 13px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  strong {
    color: #f8faff;
    font-size: 38px;
    line-height: 1;
  }
}

.summary-line {
  margin-bottom: 14px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  color: #9aa6c8;
  font-size: 12px;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 132px;
  border: 1px dashed rgba(134, 156, 255, 0.18);
  border-radius: 18px;
  color: #96a2c7;
  background: rgba(255, 255, 255, 0.02);
}

.metric-grid {
  margin-bottom: 18px;
}

.metric-card {
  min-height: 110px;
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  span {
    color: #9ca7c9;
    font-size: 13px;
  }

  strong {
    font-size: 20px;
    color: #f3f7ff;
  }
}

.detail-pre {
  padding: 16px;
  border: 1px solid rgba(134, 156, 255, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.03);
  color: #c9d2ee;
  line-height: 1.7;
}

@media (max-width: 1280px) {
  .app-shell {
    margin: 0 auto;
  }

  .page-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 840px) {
  .ban-slot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .app-shell {
    width: calc(100vw - 20px);
    padding-top: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-layout {
    grid-template-columns: 1fr;
  }

  .brand-group h1 {
    font-size: 24px;
  }
}
</style>


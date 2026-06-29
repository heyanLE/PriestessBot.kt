import { ref } from 'vue';

export type DashboardLanguage = 'en';

export const dashboardLanguage = ref<DashboardLanguage>('en');

export function setDashboardLanguage(_language: DashboardLanguage) {}

export function toggleDashboardLanguage() {}

export function translate(value: string): string {
  return value;
}

export function applyTranslations(_root: ParentNode = document.body) {}

export function startDashboardI18n(_root: ParentNode = document.body) {
  document.documentElement.lang = 'en';
  return () => {};
}

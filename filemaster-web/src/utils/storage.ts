// Local storage utilities

const PREFIX = 'filemaster_';

/**
 * Set item in localStorage
 */
export const setItem = <T>(key: string, value: T): void => {
  try {
    const serialized = JSON.stringify(value);
    localStorage.setItem(PREFIX + key, serialized);
  } catch (error) {
    console.error('Error saving to localStorage:', error);
  }
};

/**
 * Get item from localStorage
 */
export const getItem = <T>(key: string, defaultValue?: T): T | undefined => {
  try {
    const serialized = localStorage.getItem(PREFIX + key);
    if (serialized === null) return defaultValue;
    return JSON.parse(serialized) as T;
  } catch (error) {
    console.error('Error reading from localStorage:', error);
    return defaultValue;
  }
};

/**
 * Remove item from localStorage
 */
export const removeItem = (key: string): void => {
  try {
    localStorage.removeItem(PREFIX + key);
  } catch (error) {
    console.error('Error removing from localStorage:', error);
  }
};

/**
 * Clear all items with prefix
 */
export const clearAll = (): void => {
  try {
    Object.keys(localStorage)
      .filter(key => key.startsWith(PREFIX))
      .forEach(key => localStorage.removeItem(key));
  } catch (error) {
    console.error('Error clearing localStorage:', error);
  }
};

/**
 * Set session item
 */
export const setSessionItem = <T>(key: string, value: T): void => {
  try {
    const serialized = JSON.stringify(value);
    sessionStorage.setItem(PREFIX + key, serialized);
  } catch (error) {
    console.error('Error saving to sessionStorage:', error);
  }
};

/**
 * Get session item
 */
export const getSessionItem = <T>(key: string, defaultValue?: T): T | undefined => {
  try {
    const serialized = sessionStorage.getItem(PREFIX + key);
    if (serialized === null) return defaultValue;
    return JSON.parse(serialized) as T;
  } catch (error) {
    console.error('Error reading from sessionStorage:', error);
    return defaultValue;
  }
};

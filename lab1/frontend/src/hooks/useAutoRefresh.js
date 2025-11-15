import { useEffect, useRef, useMemo } from 'react';

const useAutoRefresh = (callback, intervalMs = 30000, dependencies = []) => {
  const intervalRef = useRef(null);
  const callbackRef = useRef(callback);

  // Создаем стабильный массив зависимостей
  const memoizedDeps = useMemo(() => [intervalMs, ...dependencies], [intervalMs, dependencies]);

  // Обновляем ref callback при каждом рендере
  useEffect(() => {
    callbackRef.current = callback;
  });

  useEffect(() => {
    const tick = () => {
      callbackRef.current();
    };

    // Запускаем интервал
    intervalRef.current = setInterval(tick, intervalMs);

    // Очистка при размонтировании
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, memoizedDeps);

  // Функция для ручной остановки обновлений
  const stopAutoRefresh = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  // Функция для возобновления обновлений
  const startAutoRefresh = () => {
    if (!intervalRef.current) {
      intervalRef.current = setInterval(() => {
        callbackRef.current();
      }, intervalMs);
    }
  };

  return { stopAutoRefresh, startAutoRefresh };
};

export default useAutoRefresh;
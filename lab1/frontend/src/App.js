import React, { useEffect, useState, useCallback } from "react";
import { Container, Typography, Alert, CircularProgress, Box, Snackbar, Button } from "@mui/material";
import api from "./api";
import useAutoRefresh from "./hooks/useAutoRefresh";
import Navigation from "./components/Navigation";
import RoutesTable from "./components/RoutesTable";
import RouteForm from "./components/RouteForm";
import RouteDetails from "./components/RouteDetails";
import SpecialOperations from "./components/SpecialOperations";
import ImportRoutes from "./components/ImportRoutes";
import ImportHistory from "./components/ImportHistory";

function App() {
  const [activeSection, setActiveSection] = useState('main');
  const [routes, setRoutes] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [cursorInfo, setCursorInfo] = useState(null); // Cursor-based пагинация (для мониторинга)
  const [filterName, setFilterName] = useState("");
  const [sortBy, setSortBy] = useState("id");
  const [sortDirection, setSortDirection] = useState("asc");
  const [editing, setEditing] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdateTime, setLastUpdateTime] = useState(null);
  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(true);
  const [refreshNotification, setRefreshNotification] = useState("");

  const pageSize = 10; // размер страницы

  // Загрузка маршрутов с cursor-based пагинацией
  const loadRoutes = useCallback(async (cursor = null, name = filterName, sort = sortBy, direction = sortDirection, navigateDirection = "next") => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        size: pageSize,
        sortBy: sort,
        sortDirection: direction,
        direction: navigateDirection
      };
      
      if (name && name.trim()) {
        params.nameFilter = name.trim();
      }
      
      if (cursor) {
        params.cursor = cursor;
      }
      
      const response = await api.get("/routes/cursor", { params });
      
      setRoutes(response.data.content || []);
      setTotalElements(response.data.totalCount || 0);
      setTotalPages(Math.ceil((response.data.totalCount || 0) / pageSize)); // Приблизительно для UI
      setSortBy(sort);
      setSortDirection(direction);
      setLastUpdateTime(new Date().toLocaleTimeString("ru-RU"));
      
      // Сохраняем cursor информацию для навигации
      setCursorInfo({
        next: response.data.nextCursor,
        prev: response.data.prevCursor,
        hasNext: response.data.hasNext,
        hasPrev: response.data.hasPrev
      });
      
      console.debug('🚀 Cursor pagination info:', {
        hasNext: response.data.hasNext,
        hasPrev: response.data.hasPrev,
        totalCount: response.data.totalCount,
        size: response.data.size,
        performance: 'Optimized composite cursor'
      });
      
    } catch (err) {
      console.error("Ошибка загрузки маршрутов:", err);
      setError("Не удалось загрузить маршруты. Проверьте соединение с сервером.");
      setRoutes([]);
      setTotalPages(0);
      setTotalElements(0);
      setCursorInfo(null);
    } finally {
      setLoading(false);
    }
  }, [filterName, sortBy, sortDirection, pageSize]);

  // "Тихое" обновление данных для автосинхронизации
  const silentRefresh = useCallback(async () => {
    if (activeSection !== 'main' || !autoRefreshEnabled) return;
    
    try {
      const params = {
        size: pageSize,
        sortBy: sortBy,
        sortDirection: sortDirection
      };
      if (filterName && filterName.trim()) {
        params.nameFilter = filterName.trim();
      }
      
      const response = await api.get("/routes/cursor", { params });
      const newRoutes = response.data.content || [];
      const newTotalElements = response.data.totalCount || 0;
      
      // Проверяем изменения
      if (JSON.stringify(newRoutes) !== JSON.stringify(routes) || newTotalElements !== totalElements) {
        setRoutes(newRoutes);
        setTotalPages(Math.ceil(newTotalElements / pageSize));
        setTotalElements(newTotalElements);
        setLastUpdateTime(new Date().toLocaleTimeString("ru-RU"));
        
        // Обновляем cursor информацию
        setCursorInfo({
          next: response.data.nextCursor,
          prev: response.data.prevCursor,
          hasNext: response.data.hasNext,
          hasPrev: response.data.hasPrev
        });
        
        // Показываем уведомление о обновлении
        const changeCount = Math.abs(newTotalElements - totalElements);
        if (changeCount > 0) {
          setRefreshNotification(`Данные обновлены. Изменений: ${changeCount}`);
        } else {
          setRefreshNotification("Данные обновлены");
        }
      }
    } catch (err) {
      console.error("Ошибка автоматического обновления:", err);
      // Не показываем ошибку пользователю для тихого обновления
    }
  }, [activeSection, autoRefreshEnabled, pageSize, sortBy, sortDirection, filterName, routes, totalElements]);

  // Автоматическое обновление данных каждые 30 секунд
  useAutoRefresh(silentRefresh, 30000, [activeSection, autoRefreshEnabled]);

  // Загрузка при монтировании компонента и при переключении на главную
  useEffect(() => {
    if (activeSection === 'main') {
      loadRoutes(null, "", "id", "asc", "next");
      setCurrentPage(0);
    }
  }, [activeSection]); // eslint-disable-line react-hooks/exhaustive-deps

  // Обработка cursor navigation
  const handleCursorNavigation = (direction) => {
    if (!cursorInfo) return;
    
    if (direction === "next" && cursorInfo.hasNext && cursorInfo.next) {
      loadRoutes(cursorInfo.next, filterName, sortBy, sortDirection, "next");
      setCurrentPage(prev => prev + 1);
    } else if (direction === "prev" && cursorInfo.hasPrev && cursorInfo.prev) {
      loadRoutes(cursorInfo.prev, filterName, sortBy, sortDirection, "prev");
      setCurrentPage(prev => Math.max(0, prev - 1));
    }
  };

  // Обработка изменения страницы (эмуляция через cursor)
  const handlePageChange = (page) => {
    if (page === 0) {
      // Первая страница
      loadRoutes(null, filterName, sortBy, sortDirection, "next");
      setCurrentPage(0);
    } else if (page > currentPage) {
      // Вперед
      handleCursorNavigation("next");
    } else {
      // Назад
      handleCursorNavigation("prev");
    }
  };

  // Обработка изменения фильтра
  const handleFilterChange = (name) => {
    setFilterName(name);
    setCurrentPage(0);
    loadRoutes(null, name, sortBy, sortDirection, "next");
  };

  // Обработка изменения сортировки
  const handleSortChange = (column, direction) => {
    setSortBy(column);
    setSortDirection(direction);
    setCurrentPage(0);
    loadRoutes(null, filterName, column, direction, "next");
  };

  // Удаление маршрута
  const handleDelete = async (id) => {
    if (window.confirm(`Удалить маршрут ${id}?`)) {
      try {
        await api.delete(`/routes/${id}`);
        // Перезагрузка текущей страницы, но если она стала пустой, переходим на предыдущую
        const newTotalElements = totalElements - 1;
        const newTotalPages = Math.ceil(newTotalElements / pageSize);
        const pageToLoad = currentPage >= newTotalPages ? Math.max(0, newTotalPages - 1) : currentPage;
        
        // После удаления перезагружаем первую страницу
        loadRoutes(null, filterName, sortBy, sortDirection, "next");
        setCurrentPage(0);
      } catch (err) {
        console.error("Ошибка удаления маршрута:", err);
        setError("Не удалось удалить маршрут. Попробуйте еще раз.");
      }
    }
  };

  // Сохранение маршрута (создание или обновление)
  const handleSubmit = async (values, refreshFormData) => {
    const dto = {
      name: values.name,
      distance: Number(values.distance),
      rating: Number(values.rating),
      coordinates: {
        x: values.coordinatesX === "" || values.coordinatesX === null ? null : Number(values.coordinatesX),
        y: Number(values.coordinatesY)
      },
      from: { x: Number(values.fromX), y: Number(values.fromY), name: values.fromName || null },
      to: { x: Number(values.toX), y: Number(values.toY), name: values.toName || null }
    };

    try {
      if (values.id) {
        // Обновление
        await api.put(`/routes/${values.id}`, { id: values.id, ...dto });
        setEditing(null);
      } else {
        // Создание
        await api.post("/routes", dto);
      }
      
      // Перезагрузка данных таблицы
      loadRoutes(null, filterName, sortBy, sortDirection, "next");
      setCurrentPage(0);
      
      // Обновление данных формы (координаты и локации)
      if (refreshFormData) {
        refreshFormData();
      }
    } catch (err) {
      console.error("Ошибка сохранения маршрута:", err);
      console.error("Response status:", err.response?.status);
      console.error("Response data:", err.response?.data);
      console.error("Full error:", err);
      
      // Обработка ошибок валидации уникальности
      if (err.response && err.response.status === 409) {
        const errorData = err.response.data;
        console.log("409 Conflict error data:", errorData);
        if (errorData.type === "DUPLICATE_NAME") {
          setError(`❌ ${errorData.error}`);
        } else if (errorData.type === "DUPLICATE_COORDINATES") {
          setError(`📍 ${errorData.error}`);
        } else {
          setError(`⚠️ ${errorData.error || "Конфликт данных. Проверьте уникальность введенных значений."}`);
        }
      } else if (err.response && err.response.status === 400) {
        const errorData = err.response.data;
        console.log("400 Bad Request error data:", errorData);
        if (errorData.type === "INVALID_ARGUMENT") {
          setError(`🔧 ${errorData.error}`);
        } else {
          setError(`⚠️ ${errorData.error || "Ошибка валидации данных. Проверьте корректность введенных значений."}`);
        }
      } else if (err.response && err.response.status === 500) {
        const errorData = err.response.data;
        console.log("500 Internal Server error data:", errorData);
        setError(`💥 ${errorData.error || "Внутренняя ошибка сервера. Попробуйте еще раз."}`);
      } else if (err.response && err.response.data && err.response.data.error) {
        console.log("Other response error:", err.response.data);
        setError(`❌ ${err.response.data.error}`);
      } else {
        console.log("Network or other error");
        setError("❌ Не удалось сохранить маршрут. Проверьте соединение с сервером и попробуйте еще раз.");
      }
    }
  };

  // Обработка смены секции
  const handleSectionChange = (section) => {
    setActiveSection(section);
    // Сброс состояния редактирования при переходе между секциями
    if (editing) {
      setEditing(null);
    }
    // Очистка ошибок
    setError(null);
    // Очистка уведомлений
    setRefreshNotification("");
  };

  // Рендер содержимого в зависимости от активной секции
  const renderContent = () => {
    switch (activeSection) {
      case 'main':
        return (
          <>
            <Container maxWidth="lg" sx={{ py: 2 }}>
              <Typography variant="h4" component="h1" gutterBottom align="center">
                📋 Управление маршрутами
              </Typography>
              
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
                <Typography variant="body1" color="text.secondary">
                  Создание, редактирование и удаление маршрутов
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    {lastUpdateTime && `Обновлено: ${lastUpdateTime}`}
                  </Typography>
                  <Button
                    size="small"
                    variant={autoRefreshEnabled ? "contained" : "outlined"}
                    onClick={() => setAutoRefreshEnabled(!autoRefreshEnabled)}
                    color={autoRefreshEnabled ? "success" : "default"}
                  >
                    {autoRefreshEnabled ? "🔄 Автообновление" : "⏸️ Обновление отключено"}
                  </Button>
                </Box>
              </Box>

              {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                  {error}
                </Alert>
              )}

              {loading ? (
                <Box display="flex" justifyContent="center" my={4}>
                  <CircularProgress size={60} />
                </Box>
              ) : (
                <RoutesTable
                  routes={routes}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  currentPage={currentPage}
                  filterName={filterName}
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onPageChange={handlePageChange}
                  onFilterChange={handleFilterChange}
                  onSortChange={handleSortChange}
                  onEdit={(route) => setEditing(route)}
                  onDelete={handleDelete}
                  cursorInfo={cursorInfo}
                />
              )}

              <RouteForm
                initialValues={editing ? {
                  id: editing.id,
                  name: editing.name,
                  distance: editing.distance,
                  rating: editing.rating,
                  coordinatesX: editing.coordinates.x !== null ? editing.coordinates.x : "",
                  coordinatesY: editing.coordinates.y,
                  fromX: editing.from.x,
                  fromY: editing.from.y,
                  fromName: editing.from.name || "",
                  toX: editing.to.x,
                  toY: editing.to.y,
                  toName: editing.to.name || ""
                } : {
                  name: "", distance: 2, rating: 1,
                  coordinatesX: "", coordinatesY: 0,
                  fromX: 0, fromY: 0, fromName: "",
                  toX: 0, toY: 0, toName: ""
                }}
                onSubmit={handleSubmit}
                onCancel={editing ? () => setEditing(null) : null}
              />
            </Container>
          </>
        );

      case 'details':
        return (
          <Container maxWidth="lg" sx={{ py: 2 }}>
            <RouteDetails />
          </Container>
        );

      case 'special':
        return (
          <Container maxWidth="lg" sx={{ py: 2 }}>
            <SpecialOperations />
          </Container>
        );

      case 'import':
        return (
          <Container maxWidth="lg" sx={{ py: 2 }}>
            <ImportRoutes />
          </Container>
        );

      case 'import-history':
        return (
          <Container maxWidth="lg" sx={{ py: 2 }}>
            <ImportHistory />
          </Container>
        );

      default:
        return null;
    }
  };

  return (
    <Box sx={{ 
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      paddingBottom: 2
    }}>
      <Navigation activeSection={activeSection} onSectionChange={handleSectionChange} />
      
      <Box sx={{ 
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: '16px 16px 0 0',
        minHeight: 'calc(100vh - 80px)',
        boxShadow: '0 -4px 20px rgba(0, 0, 0, 0.1)'
      }}>
        {renderContent()}
      </Box>

      {/* Уведомления об обновлениях */}
      <Snackbar
        open={!!refreshNotification}
        autoHideDuration={3000}
        onClose={() => setRefreshNotification("")}
        message={refreshNotification}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      />
    </Box>
  );
}

export default App;

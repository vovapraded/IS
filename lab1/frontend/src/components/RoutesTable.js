import React, { useState } from "react";
import {
  Table, TableHead, TableRow, TableCell, TableBody, IconButton,
  TextField, Box, Pagination, Paper, TableContainer,
  Typography, Chip, Grid, InputAdornment, Dialog, DialogTitle,
  DialogContent, DialogActions, Button, Autocomplete, Alert
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import SearchIcon from "@mui/icons-material/Search";
import ClearIcon from "@mui/icons-material/Clear";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import api from "../api";

function RoutesTable({
  routes,
  totalPages,
  totalElements,
  currentPage,
  onPageChange,
  onFilterChange,
  onSortChange,
  filterName,
  sortBy,
  sortDirection,
  onEdit,
  onDelete
}) {
  const [localFilter, setLocalFilter] = useState(filterName || "");
  const [deleteDialog, setDeleteDialog] = useState({ open: false, route: null, message: "" });
  const [rebindDialog, setRebindDialog] = useState({ open: false, route: null, dependencyInfo: null });
  const [availableRoutes, setAvailableRoutes] = useState([]);
  const [selectedTargetRoute, setSelectedTargetRoute] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    onFilterChange(localFilter);
  };

  const handleClearFilter = () => {
    setLocalFilter("");
    onFilterChange("");
  };

  const handleSort = (column) => {
    const newDirection = (sortBy === column && sortDirection === 'asc') ? 'desc' : 'asc';
    onSortChange(column, newDirection);
  };

  const getSortIcon = (column) => {
    if (sortBy !== column) return null;
    return sortDirection === 'asc' ? <ArrowUpwardIcon fontSize="small" /> : <ArrowDownwardIcon fontSize="small" />;
  };

  const handleDeleteClick = async (route) => {
    setLoading(true);
    setError(null);
    
    try {
      // Проверяем зависимости перед удалением
      const dependenciesResponse = await api.get(`/routes/${route.id}/check-dependencies`);
      const dependencyInfo = dependenciesResponse.data;
      
      if (dependencyInfo.hasSharedResources) {
        // Есть связанные объекты - показываем диалог выбора целевого маршрута
        setRebindDialog({ open: true, route, dependencyInfo });
        setAvailableRoutes(dependencyInfo.alternativeRoutes || []);
      } else {
        // Нет связанных объектов - показываем простой диалог подтверждения
        const message = `Маршрут "${route.name}" можно безопасно удалить. Его координаты и локации не используются другими маршрутами.`;
        setDeleteDialog({ open: true, route, message });
      }
    } catch (err) {
      console.error("Ошибка проверки зависимостей:", err);
      setError("Не удалось проверить зависимости маршрута");
      // В случае ошибки показываем простой диалог
      setDeleteDialog({ open: true, route });
    } finally {
      setLoading(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteDialog.route) return;
    
    try {
      await onDelete(deleteDialog.route.id);
      setDeleteDialog({ open: false, route: null });
    } catch (err) {
      console.error("Ошибка удаления:", err);
    }
  };

  const confirmDeleteWithRebind = async () => {
    if (!rebindDialog.route || !selectedTargetRoute) return;
    
    setLoading(true);
    try {
      await api.delete(`/routes/${rebindDialog.route.id}?targetRouteId=${selectedTargetRoute.id}`);
      setRebindDialog({ open: false, route: null });
      setSelectedTargetRoute(null);
      // Перезагружаем данные через родительский компонент
      window.location.reload();
    } catch (err) {
      console.error("Ошибка удаления с перепривязкой:", err);
      setError("Не удалось удалить маршрут с перепривязкой");
    } finally {
      setLoading(false);
    }
  };

  const closeDialogs = () => {
    setDeleteDialog({ open: false, route: null });
    setRebindDialog({ open: false, route: null });
    setSelectedTargetRoute(null);
    setError(null);
  };

  return (
    <Paper elevation={3} sx={{ mt: 3 }}>
      <Box sx={{ p: 2 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        <Grid container alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
          <Grid item>
            <Typography variant="h6" component="h2">
              Список маршрутов
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Всего найдено: {totalElements} маршрутов
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <Box component="form" onSubmit={handleFilterSubmit}>
              <TextField
                fullWidth
                variant="outlined"
                placeholder="Поиск по названию..."
                value={localFilter}
                onChange={(e) => setLocalFilter(e.target.value)}
                size="small"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
                  endAdornment: localFilter && (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="очистить поиск"
                        onClick={handleClearFilter}
                        edge="end"
                        size="small"
                      >
                        <ClearIcon />
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
            </Box>
          </Grid>
        </Grid>

        {filterName && (
          <Box sx={{ mb: 2 }}>
            <Chip 
              label={`Фильтр: "${filterName}"`} 
              onDelete={() => onFilterChange("")}
              color="primary" 
              variant="outlined" 
            />
          </Box>
        )}
      </Box>

      <TableContainer>
        <Table>
          <TableHead>
            <TableRow sx={{ backgroundColor: '#f5f5f5' }}>
              <TableCell
                sx={{ fontWeight: 'bold', cursor: 'pointer', userSelect: 'none' }}
                onClick={() => handleSort('id')}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  ID {getSortIcon('id')}
                </Box>
              </TableCell>
              <TableCell
                sx={{ fontWeight: 'bold', cursor: 'pointer', userSelect: 'none' }}
                onClick={() => handleSort('name')}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  Название {getSortIcon('name')}
                </Box>
              </TableCell>
              <TableCell
                sx={{ fontWeight: 'bold', cursor: 'pointer', userSelect: 'none' }}
                onClick={() => handleSort('distance')}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  Расстояние {getSortIcon('distance')}
                </Box>
              </TableCell>
              <TableCell
                sx={{ fontWeight: 'bold', cursor: 'pointer', userSelect: 'none' }}
                onClick={() => handleSort('rating')}
              >
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  Рейтинг {getSortIcon('rating')}
                </Box>
              </TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Координаты</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Откуда</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Куда</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {routes.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                  <Typography variant="body1" color="text.secondary">
                    {filterName ? `Не найдено маршрутов с названием "${filterName}"` : "Нет данных для отображения"}
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              routes.map((route) => (
                <TableRow 
                  key={route.id} 
                  hover
                  sx={{ '&:nth-of-type(odd)': { backgroundColor: '#fafafa' } }}
                >
                  <TableCell>{route.id}</TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                      {route.name}
                    </Typography>
                  </TableCell>
                  <TableCell>{route.distance}</TableCell>
                  <TableCell>
                    <Chip 
                      label={route.rating} 
                      color={route.rating >= 3 ? "success" : route.rating >= 2 ? "warning" : "error"}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                      ({route.coordinates.x}, {route.coordinates.y})
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                      ({route.from.x}, {route.from.y})
                      {route.from.name && (
                        <Typography component="span" variant="body2" color="text.secondary">
                          <br />{route.from.name}
                        </Typography>
                      )}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                      ({route.to.x}, {route.to.y})
                      {route.to.name && (
                        <Typography component="span" variant="body2" color="text.secondary">
                          <br />{route.to.name}
                        </Typography>
                      )}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <IconButton
                        color="primary"
                        onClick={() => onEdit(route)}
                        size="small"
                        title="Редактировать"
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDeleteClick(route)}
                        size="small"
                        title="Удалить"
                        disabled={loading}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
          <Pagination
            count={totalPages}
            page={currentPage + 1}
            onChange={(event, value) => onPageChange(value - 1)}
            color="primary"
            size="large"
            showFirstButton
            showLastButton
          />
        </Box>
      )}

      {/* Диалог простого удаления */}
      <Dialog open={deleteDialog.open} onClose={closeDialogs}>
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить маршрут "{deleteDialog.route?.name}"?
          </Typography>
          {deleteDialog.message && (
            <Typography variant="body2" color="success.main" sx={{ mt: 1, mb: 1 }}>
              {deleteDialog.message}
            </Typography>
          )}
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Это действие нельзя отменить.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialogs}>Отмена</Button>
          <Button onClick={confirmDelete} color="error" variant="contained">
            Удалить
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог удаления с перепривязкой */}
      <Dialog open={rebindDialog.open} onClose={closeDialogs} maxWidth="sm" fullWidth>
        <DialogTitle>Найдены связанные объекты</DialogTitle>
        <DialogContent>
          {rebindDialog.dependencyInfo && (() => {
            const { dependencyInfo } = rebindDialog;
            const sharedInfo = [];
            if (dependencyInfo.coordinatesUsageCount > 0) {
              sharedInfo.push(`координаты (используются в ${dependencyInfo.coordinatesUsageCount + 1} маршрутах)`);
            }
            if (dependencyInfo.fromLocationUsageCount > 0) {
              sharedInfo.push(`локация "откуда" (используется в ${dependencyInfo.fromLocationUsageCount + 1} маршрутах)`);
            }
            if (dependencyInfo.toLocationUsageCount > 0) {
              sharedInfo.push(`локация "куда" (используется в ${dependencyInfo.toLocationUsageCount + 1} маршрутах)`);
            }
            
            const message = `Маршрут "${rebindDialog.route?.name}" использует ${sharedInfo.join(', ')}.`;
            return (
              <Typography sx={{ mb: 2 }}>
                {message}
              </Typography>
            );
          })()}
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Система автоматически перепривяжет эти ресурсы к выбранному маршруту:
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Autocomplete
            options={availableRoutes}
            getOptionLabel={(option) => `${option.id}: ${option.name}`}
            value={selectedTargetRoute}
            onChange={(event, newValue) => setSelectedTargetRoute(newValue)}
            loading={loading}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Целевой маршрут"
                placeholder="Выберите маршрут для перепривязки..."
                required
              />
            )}
            renderOption={(props, option) => (
              <Box component="li" {...props}>
                <Box>
                  <Typography variant="body1">
                    {option.id}: {option.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    От ({option.from.x}, {option.from.y}) до ({option.to.x}, {option.to.y})
                  </Typography>
                </Box>
              </Box>
            )}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialogs}>Отмена</Button>
          <Button
            onClick={confirmDeleteWithRebind}
            color="warning"
            variant="contained"
            disabled={!selectedTargetRoute || loading}
          >
            Удалить с перепривязкой
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
}

export default RoutesTable;

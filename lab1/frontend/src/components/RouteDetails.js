import React, { useState } from "react";
import {
  Paper, Typography, TextField, Button, Box, Alert,
  Card, CardContent, Grid, Divider, Chip, CircularProgress,
  Tooltip
} from "@mui/material";
import SearchIcon from '@mui/icons-material/Search';
import CircleIcon from "@mui/icons-material/Circle";
import api from "../api";

function RouteDetails() {
  const [routeId, setRouteId] = useState("");
  const [route, setRoute] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!routeId.trim()) {
      setError("Введите ID маршрута");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await api.get(`/routes/${routeId.trim()}`);
      setRoute(response.data);
    } catch (err) {
      console.error("Ошибка получения маршрута:", err);
      if (err.response?.status === 404) {
        setError(`Маршрут с ID ${routeId} не найден`);
      } else {
        setError("Ошибка получения данных маршрута");
      }
      setRoute(null);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString("ru-RU");
  };

  // Компонент для отображения информации о владельце
  const OwnerIndicator = ({ ownerRouteId, ownerRouteName, currentRouteId, type }) => {
    const isOwnedByCurrentRoute = ownerRouteId === currentRouteId;
    
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
        <Typography>
          <strong>Владелец:</strong>
        </Typography>
        {ownerRouteId ? (
          <Box sx={{ display: 'flex', alignItems: 'center', ml: 1 }}>
            <Typography>
              Маршрут #{ownerRouteId} ({ownerRouteName})
            </Typography>
            {isOwnedByCurrentRoute && (
              <Tooltip title="Принадлежит этому маршруту" arrow>
                <CircleIcon
                  sx={{
                    color: '#ffa726',
                    fontSize: '14px',
                    ml: 0.5
                  }}
                />
              </Tooltip>
            )}
          </Box>
        ) : (
          <Typography color="text.secondary" sx={{ ml: 1 }}>
            Не назначен
          </Typography>
        )}
      </Box>
    );
  };

  return (
    <Paper elevation={3} sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Поиск маршрута по ID
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Введите ID маршрута для получения подробной информации
      </Typography>

      <Divider sx={{ mb: 3 }} />

      {/* Форма поиска */}
      <Box component="form" onSubmit={handleSearch} sx={{ mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={8} md={6}>
            <TextField
              fullWidth
              label="ID маршрута"
              value={routeId}
              onChange={(e) => setRouteId(e.target.value)}
              placeholder="Например: 123"
              type="number"
              inputProps={{ min: 1 }}
            />
          </Grid>
          <Grid item xs={12} sm={4} md={2}>
            <Button
              type="submit"
              variant="contained"
              startIcon={<SearchIcon />}
              fullWidth
              disabled={loading}
            >
              Найти
            </Button>
          </Grid>
        </Grid>
      </Box>

      {/* Ошибки */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Загрузка */}
      {loading && (
        <Box display="flex" justifyContent="center" my={3}>
          <CircularProgress />
        </Box>
      )}

      {/* Детали маршрута */}
      {route && (
        <Card elevation={2}>
          <CardContent>
            <Typography variant="h5" gutterBottom color="primary">
              {route.name}
            </Typography>
            
            <Grid container spacing={3}>
              {/* Основная информация */}
              <Grid item xs={12} md={6}>
                <Card variant="outlined" sx={{ height: '100%' }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom color="primary">
                      Основная информация
                    </Typography>
                    <Box sx={{ mt: 2 }}>
                      <Typography><strong>ID:</strong> {route.id}</Typography>
                      <Typography><strong>Название:</strong> {route.name}</Typography>
                      <Typography><strong>Расстояние:</strong> {route.distance} км</Typography>
                      <Typography>
                        <strong>Рейтинг:</strong> 
                        <Chip 
                          label={route.rating} 
                          color={route.rating >= 3 ? "success" : route.rating >= 2 ? "warning" : "error"}
                          size="small"
                          sx={{ ml: 1 }}
                        />
                      </Typography>
                      <Typography><strong>Дата создания:</strong> {formatDate(route.creationDate)}</Typography>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>

              {/* Координаты */}
              <Grid item xs={12} md={6}>
                <Card variant="outlined" sx={{ height: '100%' }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom color="primary">
                      Координаты маршрута
                    </Typography>
                    <Box sx={{ mt: 2 }}>
                      <Typography sx={{ fontFamily: 'monospace', fontSize: '1.1rem' }}>
                        X: {route.coordinates.x}
                      </Typography>
                      <Typography sx={{ fontFamily: 'monospace', fontSize: '1.1rem' }}>
                        Y: {route.coordinates.y}
                      </Typography>
                      <OwnerIndicator
                        ownerRouteId={route.coordinates.ownerRouteId}
                        ownerRouteName={route.coordinates.ownerRouteName}
                        currentRouteId={route.id}
                        type="coordinates"
                      />
                    </Box>
                  </CardContent>
                </Card>
              </Grid>

              {/* Точка отправления */}
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="h6" gutterBottom color="primary">
                      Точка отправления
                    </Typography>
                    <Box sx={{ mt: 2 }}>
                      <Typography sx={{ fontFamily: 'monospace' }}>
                        Координаты: ({route.from.x}, {route.from.y})
                      </Typography>
                      {route.from.name && (
                        <Typography>
                          <strong>Название:</strong> {route.from.name}
                        </Typography>
                      )}
                      <OwnerIndicator
                        ownerRouteId={route.from.ownerRouteId}
                        ownerRouteName={route.from.ownerRouteName}
                        currentRouteId={route.id}
                        type="from-location"
                      />
                    </Box>
                  </CardContent>
                </Card>
              </Grid>

              {/* Точка назначения */}
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="h6" gutterBottom color="primary">
                      Точка назначения
                    </Typography>
                    <Box sx={{ mt: 2 }}>
                      <Typography sx={{ fontFamily: 'monospace' }}>
                        Координаты: ({route.to.x}, {route.to.y})
                      </Typography>
                      {route.to.name && (
                        <Typography>
                          <strong>Название:</strong> {route.to.name}
                        </Typography>
                      )}
                      <OwnerIndicator
                        ownerRouteId={route.to.ownerRouteId}
                        ownerRouteName={route.to.ownerRouteName}
                        currentRouteId={route.id}
                        type="to-location"
                      />
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}
    </Paper>
  );
}

export default RouteDetails;
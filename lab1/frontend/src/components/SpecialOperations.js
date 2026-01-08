import React, { useState, useEffect } from "react";
import {
  Paper, Typography, TextField, Button, Box, Alert, Card, CardContent,
  Grid, Divider, Accordion, AccordionSummary, AccordionDetails,
  Table, TableHead, TableRow, TableCell, TableBody, MenuItem, Select, FormControl, InputLabel, Autocomplete
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import api from "../api";

function SpecialOperations() {
  const [loading, setLoading] = useState({});
  const [errors, setErrors] = useState({});
  
  // Состояния для каждой операции
  const [maxNameResult, setMaxNameResult] = useState(null);
  const [countParams, setCountParams] = useState({ rating: "" });
  const [countResult, setCountResult] = useState(null);
  const [greaterParams, setGreaterParams] = useState({ rating: "" });
  const [greaterResult, setGreaterResult] = useState([]);
  const [betweenParams, setBetweenParams] = useState({ from: "", to: "", sortBy: "name" });
  const [betweenResult, setBetweenResult] = useState([]);
  // Список всех локаций для операции поиска между локациями
  const [allLocations, setAllLocations] = useState([]);

  const setLoadingState = (operation, isLoading) => {
    setLoading(prev => ({ ...prev, [operation]: isLoading }));
  };

  const setError = (operation, error) => {
    setErrors(prev => ({ ...prev, [operation]: error }));
  };

  const clearError = (operation) => {
    setErrors(prev => ({ ...prev, [operation]: null }));
  };

  // Загрузка всех локаций
  useEffect(() => {
    const loadAllLocations = async () => {
      try {
        const response = await api.get('/routes/special/all-locations');
        
        // Форматируем локации для отображения
        const formatted = response.data.map(location => {
          if (location.name && location.name.trim()) {
            return location.name;
          } else {
            return `(${location.x}, ${location.y})`;
          }
        });
        
        // Убираем дубликаты и сортируем
        const unique = [...new Set(formatted)].sort();
        setAllLocations(unique);
      } catch (err) {
        console.error('Error loading locations:', err);
        setAllLocations([]);
      }
    };
    
    loadAllLocations();
  }, []);

  // 1. Получить объект с максимальным name
  const handleMaxName = async () => {
    setLoadingState('maxName', true);
    clearError('maxName');
    try {
      const response = await api.get('/routes/special/max-name');
      
      // Если ответ содержит message, значит маршруты не найдены
      if (response.data.message && response.data.route === null) {
        setMaxNameResult(null);
        setError('maxName', response.data.message);
      } else if (response.data.id) {
        // Если это объект маршрута напрямую
        setMaxNameResult(response.data);
      } else {
        // Если это оболочка с route внутри
        setMaxNameResult(response.data.route);
      }
    } catch (err) {
      console.error("Ошибка получения маршрута с максимальным именем:", err);
      setError('maxName', "Ошибка получения данных");
      setMaxNameResult(null);
    } finally {
      setLoadingState('maxName', false);
    }
  };

  // 2. Подсчет маршрутов с рейтингом меньше заданного
  const handleCountRatingLess = async () => {
    if (!countParams.rating) {
      setError('count', 'Введите значение рейтинга');
      return;
    }
    setLoadingState('count', true);
    clearError('count');
    try {
      const response = await api.get(`/routes/special/count-rating-less-than/${countParams.rating}`);
      setCountResult(response.data);
    } catch (err) {
      console.error("Ошибка подсчета маршрутов:", err);
      setError('count', "Ошибка получения данных");
      setCountResult(null);
    } finally {
      setLoadingState('count', false);
    }
  };

  // 3. Маршруты с рейтингом больше заданного
  const handleRatingGreater = async () => {
    if (!greaterParams.rating) {
      setError('greater', 'Введите значение рейтинга');
      return;
    }
    setLoadingState('greater', true);
    clearError('greater');
    try {
      const response = await api.get(`/routes/special/rating-greater-than/${greaterParams.rating}`);
      // Backend теперь возвращает объект с routes внутри
      setGreaterResult(response.data.routes || []);
    } catch (err) {
      console.error("Ошибка получения маршрутов с рейтингом больше заданного:", err);
      setError('greater', "Ошибка получения данных");
      setGreaterResult([]);
    } finally {
      setLoadingState('greater', false);
    }
  };

  // 4. Поиск маршрутов между локациями
  const handleFindBetweenLocations = async () => {
    if (!betweenParams.from || !betweenParams.to) {
      setError('between', 'Выберите обе локации для поиска');
      return;
    }
    setLoadingState('between', true);
    clearError('between');
    try {
      const params = {
        from: betweenParams.from,
        to: betweenParams.to,
        sortBy: betweenParams.sortBy
      };

      const response = await api.get('/routes/special/between-locations', { params });
      // Backend теперь возвращает прямо массив маршрутов
      setBetweenResult(response.data);
    } catch (err) {
      console.error("Ошибка поиска маршрутов между локациями:", err);
      setError('between', "Ошибка получения данных");
      setBetweenResult([]);
    } finally {
      setLoadingState('between', false);
    }
  };


  const renderRouteTable = (routes) => (
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>ID</TableCell>
          <TableCell>Название</TableCell>
          <TableCell>Расстояние</TableCell>
          <TableCell>Рейтинг</TableCell>
          <TableCell>От</TableCell>
          <TableCell>До</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {routes.map((route) => (
          <TableRow key={route.id}>
            <TableCell>{route.id}</TableCell>
            <TableCell>{route.name}</TableCell>
            <TableCell>{route.distance}</TableCell>
            <TableCell>{route.rating}</TableCell>
            <TableCell>
              ({route.from.x}, {route.from.y}) {route.from.name}
            </TableCell>
            <TableCell>
              ({route.to.x}, {route.to.y}) {route.to.name}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );

  return (
    <Paper elevation={3} sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Специальные операции
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Выполнение специальных операций с маршрутами
      </Typography>
      <Divider sx={{ mb: 3 }} />

      {/* Операция 1: Максимальное имя */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">1. Маршрут с максимальным названием</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box>
            <Typography variant="body2" sx={{ mb: 2 }}>
              Получить маршрут с максимальным значением поля name
            </Typography>
            <Button 
              variant="contained" 
              onClick={handleMaxName}
              startIcon={<PlayArrowIcon />}
              disabled={loading.maxName}
            >
              Выполнить
            </Button>
            
            {errors.maxName && <Alert severity="error" sx={{ mt: 2 }}>{errors.maxName}</Alert>}
            
            {maxNameResult && maxNameResult.id && (
              <Card sx={{ mt: 2 }}>
                <CardContent>
                  <Typography variant="h6">{maxNameResult.name}</Typography>
                  <Typography>ID: {maxNameResult.id}</Typography>
                  <Typography>Расстояние: {maxNameResult.distance}</Typography>
                  <Typography>Рейтинг: {maxNameResult.rating}</Typography>
                </CardContent>
              </Card>
            )}
            
            {!maxNameResult && !errors.maxName && !loading.maxName && (
              <Alert severity="info" sx={{ mt: 2 }}>
                Нажмите "Выполнить" для поиска маршрута с максимальным именем
              </Alert>
            )}
          </Box>
        </AccordionDetails>
      </Accordion>

      {/* Операция 2: Подсчет по рейтингу */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">2. Количество маршрутов с рейтингом меньше заданного</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Рейтинг"
                  type="number"
                  value={countParams.rating}
                  onChange={(e) => setCountParams({rating: e.target.value})}
                  fullWidth
                  inputProps={{ min: 1 }}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Button 
                  variant="contained" 
                  onClick={handleCountRatingLess}
                  startIcon={<PlayArrowIcon />}
                  disabled={loading.count}
                >
                  Подсчитать
                </Button>
              </Grid>
            </Grid>
            
            {errors.count && <Alert severity="error" sx={{ mt: 2 }}>{errors.count}</Alert>}
            
            {countResult && (
              <Alert severity="info" sx={{ mt: 2 }}>
                Найдено маршрутов с рейтингом меньше {countResult.threshold}: <strong>{countResult.count}</strong>
              </Alert>
            )}
          </Box>
        </AccordionDetails>
      </Accordion>

      {/* Операция 3: Рейтинг больше заданного */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">3. Маршруты с рейтингом больше заданного</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Рейтинг"
                  type="number"
                  value={greaterParams.rating}
                  onChange={(e) => setGreaterParams({rating: e.target.value})}
                  fullWidth
                  inputProps={{ min: 0 }}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Button 
                  variant="contained" 
                  onClick={handleRatingGreater}
                  startIcon={<PlayArrowIcon />}
                  disabled={loading.greater}
                >
                  Найти
                </Button>
              </Grid>
            </Grid>
            
            {errors.greater && <Alert severity="error" sx={{ mt: 2 }}>{errors.greater}</Alert>}
            
            {greaterResult.length > 0 && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>
                  Найдено маршрутов: {greaterResult.length}
                </Typography>
                {renderRouteTable(greaterResult)}
              </Box>
            )}
          </Box>
        </AccordionDetails>
      </Accordion>

      {/* Операция 4: Поиск между локациями */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">4. Поиск маршрутов между локациями</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={allLocations}
                  value={betweenParams.from}
                  onChange={(event, newValue) => {
                    setBetweenParams(prev => ({...prev, from: newValue || ""}));
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Локация отправления"
                      placeholder="Выберите локацию отправления"
                      fullWidth
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={allLocations}
                  value={betweenParams.to}
                  onChange={(event, newValue) => {
                    setBetweenParams(prev => ({...prev, to: newValue || ""}));
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Локация назначения"
                      placeholder="Выберите локацию назначения"
                      fullWidth
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControl fullWidth>
                  <InputLabel>Сортировать по</InputLabel>
                  <Select
                    value={betweenParams.sortBy}
                    onChange={(e) => setBetweenParams(prev => ({...prev, sortBy: e.target.value}))}
                  >
                    <MenuItem value="name">Названию</MenuItem>
                    <MenuItem value="distance">Расстоянию</MenuItem>
                    <MenuItem value="rating">Рейтингу</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <Button 
                  variant="contained" 
                  onClick={handleFindBetweenLocations}
                  startIcon={<PlayArrowIcon />}
                  disabled={loading.between}
                >
                  Найти маршруты
                </Button>
              </Grid>
            </Grid>
            
            {errors.between && <Alert severity="error" sx={{ mt: 2 }}>{errors.between}</Alert>}
            
            {betweenResult.length > 0 && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>
                  Найдено маршрутов: {betweenResult.length}
                </Typography>
                {renderRouteTable(betweenResult)}
              </Box>
            )}
            
            {betweenResult.length === 0 && !errors.between && !loading.between && betweenParams.from && betweenParams.to && (
              <Alert severity="info" sx={{ mt: 2 }}>
                Маршруты между указанными локациями не найдены
              </Alert>
            )}
          </Box>
        </AccordionDetails>
      </Accordion>

    </Paper>
  );
}

export default SpecialOperations;
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
  const [addParams, setAddParams] = useState({
    routeName: "", coordX: "", coordY: "", fromX: "", fromY: "", fromName: "",
    toX: "", toY: "", toName: "", distance: "", rating: ""
  });
  
  // Список всех локаций
  const [allLocations, setAllLocations] = useState([]);
  const [rawLocations, setRawLocations] = useState([]); // Исходные данные локаций
  
  // Состояния для контроля автокомплитов
  const [selectedRouteLocation, setSelectedRouteLocation] = useState("");
  const [selectedFromLocation, setSelectedFromLocation] = useState("");
  const [selectedToLocation, setSelectedToLocation] = useState("");

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
        const response = await api.get('/routes/related/all-locations');
        
        // Сохраняем исходные данные
        setRawLocations(response.data);
        
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
        setRawLocations([]);
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
      setGreaterResult(response.data);
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
      setBetweenResult(response.data);
    } catch (err) {
      console.error("Ошибка поиска маршрутов между локациями:", err);
      setError('between', "Ошибка получения данных");
      setBetweenResult([]);
    } finally {
      setLoadingState('between', false);
    }
  };

  // Функция для парсинга выбранной локации
  const parseLocationSelection = (locationString) => {
    if (!locationString) return { x: "", y: "", name: "" };
    
    // Если это координаты в формате "(x, y)"
    const coordMatch = locationString.match(/^\((-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\)$/);
    if (coordMatch) {
      return {
        x: coordMatch[1],
        y: coordMatch[2],
        name: ""
      };
    }
    
    // Если это название локации, находим её в исходных данных
    const foundLocation = rawLocations.find(loc =>
      loc.name && loc.name.trim() === locationString
    );
    
    if (foundLocation) {
      return {
        x: foundLocation.x.toString(),
        y: foundLocation.y.toString(),
        name: foundLocation.name
      };
    }
    
    // Если не нашли, возвращаем только название
    return {
      x: "",
      y: "",
      name: locationString
    };
  };

  // Обработчик выбора координат маршрута
  const handleRouteLocationSelect = (selectedLocation) => {
    setSelectedRouteLocation(selectedLocation || "");
    const parsed = parseLocationSelection(selectedLocation);
    if (parsed.x && parsed.y) {
      setAddParams(prev => ({
        ...prev,
        coordX: parsed.x,
        coordY: parsed.y
      }));
    }
  };

  // Обработчик выбора локации отправления
  const handleFromLocationSelect = (selectedLocation) => {
    setSelectedFromLocation(selectedLocation || "");
    const parsed = parseLocationSelection(selectedLocation);
    setAddParams(prev => ({
      ...prev,
      fromX: parsed.x,
      fromY: parsed.y,
      fromName: parsed.name
    }));
  };

  // Обработчик выбора локации назначения
  const handleToLocationSelect = (selectedLocation) => {
    setSelectedToLocation(selectedLocation || "");
    const parsed = parseLocationSelection(selectedLocation);
    setAddParams(prev => ({
      ...prev,
      toX: parsed.x,
      toY: parsed.y,
      toName: parsed.name
    }));
  };

  // Обработчики изменения полей - очищают автокомплит если изменились координаты
  const handleCoordFieldChange = (field, value) => {
    setAddParams(prev => ({...prev, [field]: value}));
    // Очищаем автокомплит если координаты изменились
    if (field === 'coordX' || field === 'coordY') {
      setSelectedRouteLocation("");
    }
  };

  const handleFromFieldChange = (field, value) => {
    setAddParams(prev => ({...prev, [field]: value}));
    // Очищаем автокомплит если координаты или название изменились
    if (field === 'fromX' || field === 'fromY' || field === 'fromName') {
      setSelectedFromLocation("");
    }
  };

  const handleToFieldChange = (field, value) => {
    setAddParams(prev => ({...prev, [field]: value}));
    // Очищаем автокомплит если координаты или название изменились
    if (field === 'toX' || field === 'toY' || field === 'toName') {
      setSelectedToLocation("");
    }
  };

  // 5. Добавить маршрут между локациями
  const handleAddBetweenLocations = async () => {
    const requiredFields = ['routeName', 'coordX', 'coordY', 'fromX', 'fromY', 'toX', 'toY', 'distance', 'rating'];
    const missingFields = requiredFields.filter(field => !addParams[field]);
    
    if (missingFields.length > 0) {
      setError('add', 'Заполните все обязательные поля');
      return;
    }
    
    setLoadingState('add', true);
    clearError('add');
    try {
      await api.post('/routes/special/add-between-locations', {
        routeName: addParams.routeName,
        coordX: parseFloat(addParams.coordX),
        coordY: parseFloat(addParams.coordY),
        fromX: parseFloat(addParams.fromX),
        fromY: parseFloat(addParams.fromY),
        fromName: addParams.fromName || null,
        toX: parseFloat(addParams.toX),
        toY: parseFloat(addParams.toY),
        toName: addParams.toName || null,
        distance: parseInt(addParams.distance),
        rating: parseInt(addParams.rating)
      });
      
      // Сброс формы после успешного добавления
      setAddParams({
        routeName: "", coordX: "", coordY: "", fromX: "", fromY: "", fromName: "",
        toX: "", toY: "", toName: "", distance: "", rating: ""
      });
      
      // Очищаем автокомплиты
      setSelectedRouteLocation("");
      setSelectedFromLocation("");
      setSelectedToLocation("");
      
      alert('Маршрут успешно добавлен!');
    } catch (err) {
      console.error("Ошибка добавления маршрута:", err);
      setError('add', err.response?.data?.error || "Ошибка добавления маршрута");
    } finally {
      setLoadingState('add', false);
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
        🔧 Специальные операции
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

      {/* Операция 5: Добавить маршрут */}
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">5. Добавить маршрут между локациями</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box>
            {/* Основная информация */}
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold', color: 'primary.main' }}>
              📍 Основная информация
            </Typography>
            
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12}>
                <TextField
                  label="Название маршрута"
                  value={addParams.routeName}
                  onChange={(e) => setAddParams(prev => ({...prev, routeName: e.target.value}))}
                  fullWidth
                  required
                />
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Расстояние"
                  type="number"
                  value={addParams.distance}
                  onChange={(e) => setAddParams(prev => ({...prev, distance: e.target.value}))}
                  fullWidth
                  required
                  inputProps={{ min: 2 }}
                  helperText="Минимум 2"
                />
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Рейтинг"
                  type="number"
                  value={addParams.rating}
                  onChange={(e) => setAddParams(prev => ({...prev, rating: e.target.value}))}
                  fullWidth
                  required
                  inputProps={{ min: 1, max: 5 }}
                  helperText="От 1 до 5"
                />
              </Grid>
            </Grid>

            {/* Координаты маршрута */}
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold', color: 'primary.main' }}>
              🗺️ Координаты маршрута
            </Typography>
            
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12}>
                <Autocomplete
                  options={allLocations}
                  value={selectedRouteLocation}
                  onChange={(event, newValue) => handleRouteLocationSelect(newValue)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Выбрать координаты из существующих локаций (необязательно)"
                      placeholder="Выберите локацию для копирования координат"
                      fullWidth
                    />
                  )}
                  sx={{ mb: 2 }}
                />
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <TextField
                  label="X координата"
                  type="number"
                  value={addParams.coordX}
                  onChange={(e) => handleCoordFieldChange('coordX', e.target.value)}
                  fullWidth
                  required
                />
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Y координата"
                  type="number"
                  value={addParams.coordY}
                  onChange={(e) => handleCoordFieldChange('coordY', e.target.value)}
                  fullWidth
                  required
                  inputProps={{ max: 807 }}
                  helperText="Максимум 807"
                />
              </Grid>
            </Grid>

            {/* Точка отправления */}
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold', color: 'primary.main' }}>
              🚀 Точка отправления
            </Typography>
            
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12}>
                <Autocomplete
                  options={allLocations}
                  value={selectedFromLocation}
                  onChange={(event, newValue) => handleFromLocationSelect(newValue)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Выбрать существующую локацию (необязательно)"
                      placeholder="Выберите локацию или введите координаты вручную"
                      fullWidth
                    />
                  )}
                  sx={{ mb: 2 }}
                />
              </Grid>
              
              <Grid item xs={12} sm={4}>
                <TextField
                  label="X"
                  type="number"
                  value={addParams.fromX}
                  onChange={(e) => handleFromFieldChange('fromX', e.target.value)}
                  fullWidth
                  required
                />
              </Grid>
              
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Y"
                  type="number"
                  value={addParams.fromY}
                  onChange={(e) => handleFromFieldChange('fromY', e.target.value)}
                  fullWidth
                  required
                />
              </Grid>
              
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Название (необязательно)"
                  value={addParams.fromName}
                  onChange={(e) => handleFromFieldChange('fromName', e.target.value)}
                  fullWidth
                />
              </Grid>
            </Grid>

            {/* Точка назначения */}
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold', color: 'primary.main' }}>
              🎯 Точка назначения
            </Typography>
            
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12}>
                <Autocomplete
                  options={allLocations}
                  value={selectedToLocation}
                  onChange={(event, newValue) => handleToLocationSelect(newValue)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Выбрать существующую локацию (необязательно)"
                      placeholder="Выберите локацию или введите координаты вручную"
                      fullWidth
                    />
                  )}
                  sx={{ mb: 2 }}
                />
              </Grid>
              
              <Grid item xs={12} sm={4}>
                <TextField
                  label="X"
                  type="number"
                  value={addParams.toX}
                  onChange={(e) => handleToFieldChange('toX', e.target.value)}
                  fullWidth
                  required
                />
              </Grid>
              
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Y"
                  type="number"
                  value={addParams.toY}
                  onChange={(e) => handleToFieldChange('toY', e.target.value)}
                  fullWidth
                  required
                />
              </Grid>
              
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Название (необязательно)"
                  value={addParams.toName}
                  onChange={(e) => handleToFieldChange('toName', e.target.value)}
                  fullWidth
                />
              </Grid>
            </Grid>

            {/* Кнопка добавления */}
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
              <Button
                variant="contained"
                onClick={handleAddBetweenLocations}
                startIcon={<PlayArrowIcon />}
                disabled={loading.add}
                size="large"
                sx={{ minWidth: 200 }}
              >
                Добавить маршрут
              </Button>
            </Box>
            
            {errors.add && <Alert severity="error" sx={{ mt: 2 }}>{errors.add}</Alert>}
          </Box>
        </AccordionDetails>
      </Accordion>
    </Paper>
  );
}

export default SpecialOperations;
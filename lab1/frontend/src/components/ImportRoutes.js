import React, { useState, useCallback } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  TextField,
  Alert,
  CircularProgress,
  Paper,
  Chip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  Info as InfoIcon,
  Download as DownloadIcon,
  Assignment as FileIcon
} from '@mui/icons-material';
import api from '../api';

function ImportRoutes() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [username, setUsername] = useState('');
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);
  const [showInstructions, setShowInstructions] = useState(false);

  // Образец CSV файла
  const csvSample = `name,coordinates_x,coordinates_y,from_x,from_y,from_name,to_x,to_y,to_name,distance,rating
Маршрут 1,10.5,100.0,0.0,0.0,Старт,50.0,50.0,Финиш,100,5
Маршрут 2,20.0,200.0,10.0,10.0,Начало,60.0,60.0,Конец,150,4
Тестовый маршрут,5.0,50.0,5.0,5.0,,25.0,25.0,,75,3`;

  const handleFileSelect = useCallback((event) => {
    const file = event.target.files[0];
    if (file) {
      if (file.type !== 'text/csv' && !file.name.endsWith('.csv')) {
        setError('Пожалуйста, выберите CSV файл');
        return;
      }
      if (file.size > 5 * 1024 * 1024) { // 5MB
        setError('Размер файла не должен превышать 5MB');
        return;
      }
      setSelectedFile(file);
      setError(null);
    }
  }, []);

  const handleImport = useCallback(async () => {
    if (!selectedFile) {
      setError('Пожалуйста, выберите файл для импорта');
      return;
    }
    
    if (!username.trim()) {
      setError('Пожалуйста, введите имя пользователя');
      return;
    }

    setImporting(true);
    setError(null);
    setImportResult(null);

    try {
      // Читаем содержимое файла
      const fileContent = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => resolve(e.target.result);
        reader.onerror = (e) => reject(e);
        reader.readAsText(selectedFile, 'utf-8');
      });

      // Отправляем запрос на импорт
      const requestData = {
        username: username.trim(),
        filename: selectedFile.name,
        fileContent: fileContent
      };

      const response = await api.post('/import/routes', requestData);
      setImportResult(response.data);
      
      // Очищаем форму при успешном импорте
      if (response.data.status === 'SUCCESS') {
        setSelectedFile(null);
        setUsername('');
        // Сбрасываем input file
        const fileInput = document.getElementById('file-input');
        if (fileInput) fileInput.value = '';
      }

    } catch (err) {
      console.error('Import error:', err);
      if (err.response && err.response.data) {
        if (typeof err.response.data === 'string') {
          setError(err.response.data);
        } else if (err.response.data.error) {
          setError(err.response.data.error);
        } else if (err.response.data.message) {
          setError(err.response.data.message);
        } else {
          setImportResult(err.response.data);
        }
      } else {
        setError('Ошибка подключения к серверу');
      }
    } finally {
      setImporting(false);
    }
  }, [selectedFile, username]);

  const downloadSample = useCallback(() => {
    const blob = new Blob([csvSample], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'sample_routes.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [csvSample]);

  const getStatusColor = (status) => {
    switch (status) {
      case 'SUCCESS': return 'success';
      case 'FAILED': return 'error';
      case 'IN_PROGRESS': return 'warning';
      default: return 'default';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'SUCCESS': return <SuccessIcon />;
      case 'FAILED': return <ErrorIcon />;
      case 'IN_PROGRESS': return <CircularProgress size={16} />;
      default: return <InfoIcon />;
    }
  };

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom align="center">
        📁 Импорт маршрутов
      </Typography>
      
      <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 4 }}>
        Массовое добавление маршрутов из CSV файла
      </Typography>

      {/* Инструкции */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">
              📋 Формат файла
            </Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button 
                variant="outlined" 
                size="small"
                startIcon={<DownloadIcon />}
                onClick={downloadSample}
              >
                Скачать образец
              </Button>
              <Button 
                variant="outlined" 
                size="small"
                onClick={() => setShowInstructions(true)}
              >
                Подробная инструкция
              </Button>
            </Box>
          </Box>
          
          <Typography variant="body2" color="text.secondary">
            CSV файл должен содержать следующие колонки:
          </Typography>
          <Paper sx={{ p: 2, mt: 1, backgroundColor: 'grey.50' }}>
            <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
              name, coordinates_x, coordinates_y, from_x, from_y, from_name, to_x, to_y, to_name, distance, rating
            </Typography>
          </Paper>
        </CardContent>
      </Card>

      {/* Форма импорта */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            🚀 Импорт данных
          </Typography>
          
          {/* Поле имени пользователя */}
          <TextField
            fullWidth
            label="Имя пользователя"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            sx={{ mb: 2 }}
            helperText="Введите ваше имя пользователя для отслеживания операций импорта"
            disabled={importing}
          />

          {/* Выбор файла */}
          <Box sx={{ mb: 2 }}>
            <input
              accept=".csv"
              style={{ display: 'none' }}
              id="file-input"
              type="file"
              onChange={handleFileSelect}
              disabled={importing}
            />
            <label htmlFor="file-input">
              <Button
                variant="outlined"
                component="span"
                startIcon={<FileIcon />}
                fullWidth
                sx={{ mb: 1 }}
                disabled={importing}
              >
                Выбрать CSV файл
              </Button>
            </label>
            
            {selectedFile && (
              <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
                <FileIcon sx={{ mr: 1, color: 'primary.main' }} />
                <Typography variant="body2">
                  {selectedFile.name} ({Math.round(selectedFile.size / 1024)} KB)
                </Typography>
              </Box>
            )}
          </Box>

          {/* Кнопка импорта */}
          <Button
            variant="contained"
            onClick={handleImport}
            disabled={!selectedFile || !username.trim() || importing}
            startIcon={importing ? <CircularProgress size={16} /> : <UploadIcon />}
            fullWidth
            size="large"
          >
            {importing ? 'Импорт в процессе...' : 'Начать импорт'}
          </Button>
        </CardContent>
      </Card>

      {/* Ошибки */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Результат импорта */}
      {importResult && (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <Chip 
                icon={getStatusIcon(importResult.status)}
                label={importResult.status}
                color={getStatusColor(importResult.status)}
                sx={{ mr: 2 }}
              />
              <Typography variant="h6">
                Результат импорта
              </Typography>
            </Box>

            <Typography variant="body1" sx={{ mb: 2 }}>
              {importResult.message}
            </Typography>

            {/* Статистика */}
            <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
              <Chip 
                label={`Всего записей: ${importResult.totalRecords || 0}`} 
                variant="outlined" 
              />
              {importResult.successfulRecords !== undefined && (
                <Chip 
                  label={`Успешно: ${importResult.successfulRecords}`} 
                  color="success" 
                  variant="outlined" 
                />
              )}
              {importResult.failedRecords !== undefined && importResult.failedRecords > 0 && (
                <Chip 
                  label={`Ошибок: ${importResult.failedRecords}`} 
                  color="error" 
                  variant="outlined" 
                />
              )}
            </Box>

            {/* Список ошибок */}
            {importResult.errors && importResult.errors.length > 0 && (
              <>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" color="error" gutterBottom>
                  Обнаруженные ошибки:
                </Typography>
                <List dense>
                  {importResult.errors.slice(0, 10).map((error, index) => (
                    <ListItem key={index}>
                      <ListItemIcon>
                        <ErrorIcon color="error" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary={error}
                        sx={{ '& .MuiListItemText-primary': { fontSize: '0.875rem' } }}
                      />
                    </ListItem>
                  ))}
                  {importResult.errors.length > 10 && (
                    <ListItem>
                      <ListItemIcon>
                        <InfoIcon color="warning" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText 
                        primary={`... и еще ${importResult.errors.length - 10} ошибок`}
                        sx={{ '& .MuiListItemText-primary': { fontSize: '0.875rem', fontStyle: 'italic' } }}
                      />
                    </ListItem>
                  )}
                </List>
              </>
            )}
          </CardContent>
        </Card>
      )}

      {/* Диалог с подробными инструкциями */}
      <Dialog 
        open={showInstructions} 
        onClose={() => setShowInstructions(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          📋 Подробная инструкция по импорту
        </DialogTitle>
        <DialogContent>
          <Typography variant="h6" gutterBottom>
            Формат CSV файла:
          </Typography>
          <List>
            <ListItem>
              <ListItemText 
                primary="name - название маршрута (обязательно, уникально)"
                secondary="Строка от 1 до 100 символов, только буквы, цифры, пробелы, _ и -"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="coordinates_x - X координата маршрута"
                secondary="Число (может быть дробным)"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="coordinates_y - Y координата маршрута (обязательно)"
                secondary="Число <= 807"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="from_x, from_y - координаты начальной точки (обязательно)"
                secondary="from_x не может быть null"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="from_name - название начальной точки (опционально)"
                secondary="Может быть пустым"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="to_x, to_y - координаты конечной точки (обязательно)"
                secondary="Координаты конечной точки"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="to_name - название конечной точки (опционально)"
                secondary="Может быть пустым"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="distance - расстояние маршрута (обязательно)"
                secondary="Целое число >= 2"
              />
            </ListItem>
            <ListItem>
              <ListItemText 
                primary="rating - рейтинг маршрута (обязательно)"
                secondary="Целое число > 0"
              />
            </ListItem>
          </List>
          
          <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
            Особенности:
          </Typography>
          <List>
            <ListItem>
              <ListItemText primary="• Первая строка должна содержать заголовки столбцов" />
            </ListItem>
            <ListItem>
              <ListItemText primary="• Все имена маршрутов должны быть уникальными" />
            </ListItem>
            <ListItem>
              <ListItemText primary="• Начальная и конечная точки не могут быть одинаковыми" />
            </ListItem>
            <ListItem>
              <ListItemText primary="• При возникновении любой ошибки весь импорт отменяется" />
            </ListItem>
            <ListItem>
              <ListItemText primary="• Максимальный размер файла: 5MB" />
            </ListItem>
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowInstructions(false)}>
            Понятно
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default ImportRoutes;
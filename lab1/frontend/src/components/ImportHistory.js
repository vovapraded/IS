import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Pagination,
  TextField,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider
} from '@mui/material';
import {
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  HourglassEmpty as InProgressIcon,
  Visibility as ViewIcon,
  Person as PersonIcon,
  CalendarToday as DateIcon,
  Assignment as FileIcon,
  TrendingUp as StatsIcon
} from '@mui/icons-material';
import api from '../api';

function ImportHistory() {
  const [operations, setOperations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [username, setUsername] = useState('');
  const [selectedOperation, setSelectedOperation] = useState(null);
  const [showDetails, setShowDetails] = useState(false);
  const [stats, setStats] = useState(null);

  const pageSize = 10;

  // Загрузка истории импорта
  const loadHistory = useCallback(async (page = 0) => {
    // Не загружаем историю если имя пользователя не указано
    if (!username.trim()) {
      setOperations([]);
      setTotalPages(0);
      setTotalCount(0);
      setCurrentPage(0);
      return;
    }

    setLoading(true);
    setError(null);
    
    try {
      const params = {
        page,
        size: pageSize,
        username: username.trim()
      };
      
      const response = await api.get('/import/history', { params });
      
      setOperations(response.data.operations || []);
      setTotalPages(Math.ceil(response.data.totalCount / pageSize));
      setTotalCount(response.data.totalCount);
      setCurrentPage(page);
      
    } catch (err) {
      console.error('Error loading import history:', err);
      setError('Не удалось загрузить историю импорта');
      setOperations([]);
    } finally {
      setLoading(false);
    }
  }, [username, pageSize]);

  // Загрузка статистики
  const loadStats = useCallback(async () => {
    // Не загружаем статистику если имя пользователя не указано
    if (!username.trim()) {
      setStats(null);
      return;
    }

    try {
      const params = {
        username: username.trim()
      };
      
      const response = await api.get('/import/stats', { params });
      setStats(response.data);
    } catch (err) {
      console.error('Error loading stats:', err);
      setStats(null);
    }
  }, [username]);

  // Загрузка детальной информации об операции
  const loadOperationDetails = async (operationId) => {
    try {
      const response = await api.get(`/import/operations/${operationId}`);
      setSelectedOperation(response.data);
      setShowDetails(true);
    } catch (err) {
      console.error('Error loading operation details:', err);
      setError('Не удалось загрузить детали операции');
    }
  };

  // Эффекты
  useEffect(() => {
    loadHistory(0);
    loadStats();
  }, [loadHistory, loadStats]);

  // Обработчики
  const handlePageChange = (event, page) => {
    loadHistory(page - 1); // Material-UI Pagination начинается с 1
  };

  const handleUsernameChange = (event) => {
    setUsername(event.target.value);
  };


  const handleSearch = () => {
    if (username.trim()) {
      setError(null); // Очищаем предыдущие ошибки
      loadHistory(0);
      loadStats();
    } else {
      setError('Необходимо указать имя пользователя');
    }
  };

  // Обработка нажатия Enter в поле ввода
  const handleUsernameKeyPress = (event) => {
    if (event.key === 'Enter') {
      handleSearch();
    }
  };

  // Форматирование даты
  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  // Получение иконки статуса
  const getStatusIcon = (status) => {
    switch (status) {
      case 'SUCCESS': return <SuccessIcon color="success" />;
      case 'FAILED': return <ErrorIcon color="error" />;
      case 'IN_PROGRESS': return <InProgressIcon color="warning" />;
      default: return <InProgressIcon />;
    }
  };

  // Получение цвета статуса
  const getStatusColor = (status) => {
    switch (status) {
      case 'SUCCESS': return 'success';
      case 'FAILED': return 'error';
      case 'IN_PROGRESS': return 'warning';
      default: return 'default';
    }
  };

  // Получение продолжительности операции
  const getDuration = (startTime, endTime) => {
    if (!startTime || !endTime) return '-';
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diffMs = end - start;
    const diffSec = Math.floor(diffMs / 1000);
    
    if (diffSec < 60) return `${diffSec} сек`;
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin} мин ${diffSec % 60} сек`;
    const diffHour = Math.floor(diffMin / 60);
    return `${diffHour} ч ${diffMin % 60} мин`;
  };

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom align="center">
        📊 История импорта
      </Typography>
      
      <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 4 }}>
        Просмотр операций импорта маршрутов для конкретного пользователя
      </Typography>

      {/* Статистика */}
      {stats && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <StatsIcon sx={{ mr: 1 }} />
              <Typography variant="h6">
                Статистика импорта
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
              <Chip 
                label={`Всего операций: ${stats.totalOperations}`} 
                variant="outlined"
                icon={<FileIcon />}
              />
              <Chip 
                label={`Успешных: ${stats.successfulOperations}`} 
                color="success"
                variant="outlined"
                icon={<SuccessIcon />}
              />
              <Chip 
                label={`Неудачных: ${stats.failedOperations}`} 
                color="error"
                variant="outlined"
                icon={<ErrorIcon />}
              />
              {stats.totalOperations > 0 && (
                <Chip 
                  label={`Успешность: ${Math.round((stats.successfulOperations / stats.totalOperations) * 100)}%`} 
                  color="info"
                  variant="outlined"
                />
              )}
            </Box>
          </CardContent>
        </Card>
      )}

      {/* Фильтры */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            🔍 Поиск операций
          </Typography>
          
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
            <TextField
              label="Имя пользователя"
              value={username}
              onChange={handleUsernameChange}
              onKeyPress={handleUsernameKeyPress}
              size="small"
              sx={{ minWidth: 200 }}
              required
              placeholder="Введите имя пользователя"
            />
            
            <Button
              variant="contained"
              onClick={handleSearch}
              disabled={!username.trim()}
            >
              Найти операции
            </Button>
          </Box>
          
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
            Отображаются только операции указанного пользователя
          </Typography>
        </CardContent>
      </Card>

      {/* Ошибки */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Таблица истории */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            📋 История операций
          </Typography>
          
          {loading ? (
            <Box display="flex" justifyContent="center" my={4}>
              <CircularProgress />
            </Box>
          ) : operations.length === 0 ? (
            <Typography variant="body1" color="text.secondary" align="center" sx={{ py: 4 }}>
              {!username.trim()
                ? 'Введите имя пользователя для просмотра истории'
                : 'Операции импорта для данного пользователя не найдены'
              }
            </Typography>
          ) : (
            <>
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>ID</TableCell>
                      <TableCell>Статус</TableCell>
                      <TableCell>Пользователь</TableCell>
                      <TableCell>Файл</TableCell>
                      <TableCell>Время начала</TableCell>
                      <TableCell>Длительность</TableCell>
                      <TableCell>Записей добавлено</TableCell>
                      <TableCell>Действия</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {operations.map((operation) => (
                      <TableRow key={operation.id} hover>
                        <TableCell>#{operation.id}</TableCell>
                        <TableCell>
                          <Chip
                            icon={getStatusIcon(operation.status)}
                            label={operation.status}
                            color={getStatusColor(operation.status)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <PersonIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} />
                            {operation.username}
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <FileIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} />
                            {operation.filename}
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <DateIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} />
                            {formatDate(operation.startTime)}
                          </Box>
                        </TableCell>
                        <TableCell>
                          {getDuration(operation.startTime, operation.endTime)}
                        </TableCell>
                        <TableCell>
                          {operation.status === 'SUCCESS' && operation.successfulRecords !== undefined
                            ? operation.successfulRecords
                            : '-'
                          }
                        </TableCell>
                        <TableCell>
                          <Button
                            size="small"
                            startIcon={<ViewIcon />}
                            onClick={() => loadOperationDetails(operation.id)}
                          >
                            Детали
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {/* Пагинация */}
              {totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
                  <Pagination
                    count={totalPages}
                    page={currentPage + 1}
                    onChange={handlePageChange}
                    color="primary"
                    showFirstButton
                    showLastButton
                  />
                </Box>
              )}

              {/* Информация о количестве записей */}
              <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 2 }}>
                Показано {operations.length} из {totalCount} записей
              </Typography>
            </>
          )}
        </CardContent>
      </Card>

      {/* Диалог с деталями операции */}
      <Dialog 
        open={showDetails} 
        onClose={() => setShowDetails(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Детали операции импорта #{selectedOperation?.id}
        </DialogTitle>
        <DialogContent>
          {selectedOperation && (
            <>
              <List>
                <ListItem>
                  <ListItemIcon><PersonIcon /></ListItemIcon>
                  <ListItemText primary="Пользователь" secondary={selectedOperation.username} />
                </ListItem>
                <ListItem>
                  <ListItemIcon><FileIcon /></ListItemIcon>
                  <ListItemText primary="Файл" secondary={selectedOperation.filename} />
                </ListItem>
                <ListItem>
                  <ListItemIcon>{getStatusIcon(selectedOperation.status)}</ListItemIcon>
                  <ListItemText 
                    primary="Статус" 
                    secondary={selectedOperation.status}
                  />
                </ListItem>
                <ListItem>
                  <ListItemIcon><DateIcon /></ListItemIcon>
                  <ListItemText 
                    primary="Время начала" 
                    secondary={formatDate(selectedOperation.startTime)}
                  />
                </ListItem>
                {selectedOperation.endTime && (
                  <ListItem>
                    <ListItemIcon><DateIcon /></ListItemIcon>
                    <ListItemText 
                      primary="Время окончания" 
                      secondary={formatDate(selectedOperation.endTime)}
                    />
                  </ListItem>
                )}
                <ListItem>
                  <ListItemText 
                    primary="Длительность" 
                    secondary={getDuration(selectedOperation.startTime, selectedOperation.endTime)}
                  />
                </ListItem>
              </List>

              <Divider sx={{ my: 2 }} />

              <Typography variant="h6" gutterBottom>
                Статистика обработки:
              </Typography>
              
              <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
                {selectedOperation.totalRecords !== undefined && (
                  <Chip label={`Всего записей: ${selectedOperation.totalRecords}`} />
                )}
                {selectedOperation.processedRecords !== undefined && (
                  <Chip label={`Обработано: ${selectedOperation.processedRecords}`} />
                )}
                {selectedOperation.successfulRecords !== undefined && (
                  <Chip 
                    label={`Успешно: ${selectedOperation.successfulRecords}`} 
                    color="success"
                  />
                )}
              </Box>

              {selectedOperation.errorMessage && (
                <>
                  <Typography variant="h6" color="error" gutterBottom>
                    Ошибка:
                  </Typography>
                  <Paper sx={{ p: 2, bgcolor: 'error.50', border: '1px solid', borderColor: 'error.200' }}>
                    <Typography variant="body2" color="error">
                      {selectedOperation.errorMessage}
                    </Typography>
                  </Paper>
                </>
              )}
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowDetails(false)}>
            Закрыть
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default ImportHistory;
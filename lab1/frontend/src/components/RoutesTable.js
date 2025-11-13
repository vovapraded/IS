import React, { useState } from "react";
import {
  Table, TableHead, TableRow, TableCell, TableBody, IconButton,
  TextField, Box, Pagination, Paper, TableContainer, 
  Typography, Chip, Grid, InputAdornment
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import SearchIcon from "@mui/icons-material/Search";
import ClearIcon from "@mui/icons-material/Clear";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";

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

  return (
    <Paper elevation={3} sx={{ mt: 3 }}>
      <Box sx={{ p: 2 }}>
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
                        onClick={() => onDelete(route.id)}
                        size="small"
                        title="Удалить"
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
    </Paper>
  );
}

export default RoutesTable;

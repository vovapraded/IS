import React from "react";
import { useFormik } from "formik";
import * as Yup from "yup";
import {
  Button, TextField, Grid, Typography, Box, Paper, 
  Card, CardContent, CardActions, Divider, Accordion, AccordionSummary, AccordionDetails
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import SaveIcon from '@mui/icons-material/Save';
import AddIcon from '@mui/icons-material/Add';
import CancelIcon from '@mui/icons-material/Cancel';

// Схема валидации с улучшенными сообщениями
const validationSchema = Yup.object().shape({
  name: Yup.string()
    .min(1, "Название не может быть пустым")
    .required("Название обязательно"),
  distance: Yup.number()
    .min(2, "Расстояние должно быть больше 1 (минимум 2)")
    .required("Расстояние обязательно"),
  rating: Yup.number()
    .positive("Рейтинг должен быть больше 0")
    .required("Рейтинг обязателен"),
  coordinatesX: Yup.number()
    .typeError("Координата X должна быть числом")
    .nullable(),
  coordinatesY: Yup.number()
    .max(807, "Координата Y не должна превышать 807")
    .typeError("Координата Y должна быть числом")
    .required("Координата Y обязательна"),
  fromX: Yup.number()
    .typeError("Координата X точки отправления должна быть числом")
    .required("Координата X точки отправления обязательна"),
  fromY: Yup.number()
    .typeError("Координата Y точки отправления должна быть числом")
    .required("Координата Y точки отправления обязательна"),
  toX: Yup.number()
    .typeError("Координата X точки назначения должна быть числом")
    .required("Координата X точки назначения обязательна"),
  toY: Yup.number()
    .typeError("Координата Y точки назначения должна быть числом")
    .required("Координата Y точки назначения обязательна")
});

function RouteForm({ initialValues, onSubmit, onCancel }) {
  const formik = useFormik({
    initialValues,
    validationSchema,
    enableReinitialize: true,
    onSubmit
  });

  const isEditing = !!initialValues.id;

  return (
    <Paper elevation={3} sx={{ mt: 4 }}>
      <Box sx={{ p: 3 }}>
        <Typography variant="h5" component="h2" gutterBottom>
          {isEditing ? "✏️ Редактировать маршрут" : "➕ Добавить новый маршрут"}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {isEditing ? "Внесите изменения в данные маршрута" : "Заполните все обязательные поля для создания нового маршрута"}
        </Typography>
        
        <Divider sx={{ mb: 3 }} />
        
        <form onSubmit={formik.handleSubmit}>
          {/* Основная информация */}
          <Card variant="outlined" sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom color="primary">
                📋 Основная информация
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <TextField 
                    fullWidth 
                    name="name" 
                    label="Название маршрута"
                    placeholder="Например: Горная тропа"
                    value={formik.values.name} 
                    onChange={formik.handleChange}
                    error={formik.touched.name && !!formik.errors.name} 
                    helperText={formik.touched.name && formik.errors.name}
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <TextField
                    fullWidth
                    type="number"
                    name="distance"
                    label="Расстояние (≥2)"
                    placeholder="Минимум 2"
                    value={formik.values.distance}
                    onChange={formik.handleChange}
                    error={formik.touched.distance && !!formik.errors.distance}
                    helperText={formik.touched.distance && formik.errors.distance}
                    inputProps={{ min: 2, step: 1 }}
                  />
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <TextField
                    fullWidth
                    type="number"
                    name="rating"
                    label="Рейтинг (≥1)"
                    placeholder="Минимум 1"
                    value={formik.values.rating}
                    onChange={formik.handleChange}
                    error={formik.touched.rating && !!formik.errors.rating}
                    helperText={formik.touched.rating && formik.errors.rating}
                    inputProps={{ min: 1, step: 1 }}
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          {/* Координаты маршрута */}
          <Accordion sx={{ mb: 3 }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="h6" color="primary">
                🎯 Координаты маршрута
              </Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    type="number"
                    name="coordinatesX"
                    label="Координата X (опционально)"
                    placeholder="Оставьте пустым для null"
                    value={formik.values.coordinatesX}
                    onChange={formik.handleChange}
                    error={formik.touched.coordinatesX && !!formik.errors.coordinatesX}
                    helperText={formik.touched.coordinatesX && formik.errors.coordinatesX}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField 
                    fullWidth 
                    type="number" 
                    name="coordinatesY" 
                    label="Координата Y (≤ 807)"
                    value={formik.values.coordinatesY} 
                    onChange={formik.handleChange}
                    error={formik.touched.coordinatesY && !!formik.errors.coordinatesY}
                    helperText={formik.touched.coordinatesY && formik.errors.coordinatesY}
                    inputProps={{ max: 807 }}
                  />
                </Grid>
              </Grid>
            </AccordionDetails>
          </Accordion>

          {/* Точка отправления */}
          <Accordion sx={{ mb: 3 }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="h6" color="primary">
                🚀 Точка отправления
              </Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={4}>
                  <TextField 
                    fullWidth 
                    name="fromX" 
                    label="X координата" 
                    type="number" 
                    value={formik.values.fromX} 
                    onChange={formik.handleChange}
                    error={formik.touched.fromX && !!formik.errors.fromX}
                    helperText={formik.touched.fromX && formik.errors.fromX}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField 
                    fullWidth 
                    name="fromY" 
                    label="Y координата" 
                    type="number" 
                    value={formik.values.fromY} 
                    onChange={formik.handleChange}
                    error={formik.touched.fromY && !!formik.errors.fromY}
                    helperText={formik.touched.fromY && formik.errors.fromY}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField 
                    fullWidth 
                    name="fromName" 
                    label="Название (опционально)" 
                    placeholder="Например: База отдыха"
                    value={formik.values.fromName} 
                    onChange={formik.handleChange}
                  />
                </Grid>
              </Grid>
            </AccordionDetails>
          </Accordion>

          {/* Точка назначения */}
          <Accordion sx={{ mb: 3 }}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="h6" color="primary">
                🏁 Точка назначения
              </Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={4}>
                  <TextField 
                    fullWidth 
                    name="toX" 
                    label="X координата" 
                    type="number" 
                    value={formik.values.toX} 
                    onChange={formik.handleChange}
                    error={formik.touched.toX && !!formik.errors.toX}
                    helperText={formik.touched.toX && formik.errors.toX}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField 
                    fullWidth 
                    name="toY" 
                    label="Y координата" 
                    type="number" 
                    value={formik.values.toY} 
                    onChange={formik.handleChange}
                    error={formik.touched.toY && !!formik.errors.toY}
                    helperText={formik.touched.toY && formik.errors.toY}
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField 
                    fullWidth 
                    name="toName" 
                    label="Название (опционально)" 
                    placeholder="Например: Вершина горы"
                    value={formik.values.toName} 
                    onChange={formik.handleChange}
                  />
                </Grid>
              </Grid>
            </AccordionDetails>
          </Accordion>

          {/* Кнопки действий */}
          <Card variant="outlined">
            <CardActions sx={{ justifyContent: 'flex-end', p: 2 }}>
              <Button 
                type="submit" 
                variant="contained" 
                size="large"
                startIcon={isEditing ? <SaveIcon /> : <AddIcon />}
                sx={{ mr: 1 }}
              >
                {isEditing ? "Сохранить изменения" : "Добавить маршрут"}
              </Button>
              {onCancel && (
                <Button 
                  variant="outlined" 
                  onClick={onCancel}
                  startIcon={<CancelIcon />}
                  size="large"
                >
                  Отмена
                </Button>
              )}
            </CardActions>
          </Card>
        </form>
      </Box>
    </Paper>
  );
}

export default RouteForm;

import React from "react";
import {
  AppBar, Toolbar, Typography, Button, Box, Container
} from "@mui/material";
import HomeIcon from '@mui/icons-material/Home';
import SearchIcon from '@mui/icons-material/Search';
import SettingsIcon from '@mui/icons-material/Settings';

function Navigation({ activeSection, onSectionChange }) {
  const menuItems = [
    { id: 'main', label: 'Главная', icon: <HomeIcon />, description: 'Управление маршрутами' },
    { id: 'details', label: 'Поиск по ID', icon: <SearchIcon />, description: 'Найти маршрут по ID' },
    { id: 'special', label: 'Специальные операции', icon: <SettingsIcon />, description: 'Дополнительные функции' }
  ];

  return (
    <AppBar position="static" sx={{ mb: 3 }}>
      <Container maxWidth="lg">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            🗺️ Система управления маршрутами
          </Typography>
          
          <Box sx={{ display: 'flex', gap: 1 }}>
            {menuItems.map((item) => (
              <Button
                key={item.id}
                color={activeSection === item.id ? 'secondary' : 'inherit'}
                onClick={() => onSectionChange(item.id)}
                startIcon={item.icon}
                variant={activeSection === item.id ? 'contained' : 'text'}
                sx={{ 
                  color: activeSection === item.id ? 'white' : 'inherit',
                  backgroundColor: activeSection === item.id ? 'rgba(255, 255, 255, 0.2)' : 'transparent'
                }}
                title={item.description}
              >
                {item.label}
              </Button>
            ))}
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
}

export default Navigation;
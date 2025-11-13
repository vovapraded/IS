package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.*;
import org.example.entity.Route;
import org.example.mapper.RouteMapper;
import org.example.repository.RouteRepositoryMB;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Stateless
public class RouteServiceMB {

    @Inject
    private RouteRepositoryMB routeRepository;

    public RouteDto createRoute(RouteCreateDto dto) {
        log.info("Creating route {}", dto);
        Route entity = RouteMapper.fromCreateDto(dto);
        return RouteMapper.toDto(routeRepository.save(entity));
    }

    public RouteDto findById(Integer id) {
        Route route = routeRepository.findById(id);
        if (route == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }
        return RouteMapper.toDto(route);
    }

    public List<RouteDto> findAll() {
        return routeRepository.findAll().stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<RouteDto> findAll(int page, int size, String nameFilter, String sortBy, String sortDirection) {
        int offset = page * size;
        return routeRepository.findAllWithFilter(offset, size, nameFilter, sortBy, sortDirection).stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public long countAll() {
        return routeRepository.countAll();
    }

    public long countWithFilter(String nameFilter) {
        return routeRepository.countWithFilter(nameFilter);
    }

    public RouteDto updateRoute(RouteUpdateDto dto) {
        log.info("Updating route {}", dto);
        try {
            Route updated = routeRepository.updateFromDto(dto);
            return RouteMapper.toDto(updated);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update route: {}", e.getMessage());
            throw e;
        }
    }

    public void delete(Integer id) {
        log.info("Deleting route with id {}", id);
        routeRepository.deleteById(id);
    }

    // Специальные операции согласно ТЗ

    public RouteDto findRouteWithMaxName() {
        log.info("Finding route with maximum name");
        Route route = routeRepository.findRouteWithMaxName();
        return route != null ? RouteMapper.toDto(route) : null;
    }

    public long countRoutesWithRatingLessThan(Long ratingThreshold) {
        log.info("Counting routes with rating less than {}", ratingThreshold);
        return routeRepository.countRoutesWithRatingLessThan(ratingThreshold);
    }

    public List<RouteDto> findRoutesWithRatingGreaterThan(Long ratingThreshold) {
        log.info("Finding routes with rating greater than {}", ratingThreshold);
        return routeRepository.findRoutesWithRatingGreaterThan(ratingThreshold).stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<RouteDto> findRoutesBetweenLocations(String fromLocationName, String toLocationName, String sortBy) {
        log.info("Finding routes between {} and {} sorted by {}", fromLocationName, toLocationName, sortBy);
        return routeRepository.findRoutesBetweenLocations(fromLocationName, toLocationName, sortBy).stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public RouteDto addRouteBetweenLocations(String routeName, float coordX, Double coordY,
                                           Double fromX, double fromY, String fromName,
                                           Double toX, double toY, String toName,
                                           Long distance, Long rating) {
        log.info("Adding route between locations {} and {}", fromName, toName);
        Integer newRouteId = routeRepository.addRouteBetweenLocations(
                routeName, coordX, coordY, fromX, fromY, fromName,
                toX, toY, toName, distance, rating);
        
        Route createdRoute = routeRepository.findById(newRouteId);
        return createdRoute != null ? RouteMapper.toDto(createdRoute) : null;
    }
}

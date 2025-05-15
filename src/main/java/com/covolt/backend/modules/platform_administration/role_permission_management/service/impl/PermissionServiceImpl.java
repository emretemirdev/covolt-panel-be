package com.covolt.backend.modules.platform_administration.role_permission_management.service.impl;

import com.covolt.backend.core.exception.DuplicateResourceException;
import com.covolt.backend.core.exception.ErrorCode;
import com.covolt.backend.core.exception.ResourceNotFoundException;
// import com.covolt.backend.core.exception.ResourceInUseException; // Eğer oluşturacaksak
import com.covolt.backend.core.model.Permission;
import com.covolt.backend.core.repository.PermissionRepository;
import com.covolt.backend.core.repository.RoleRepository; // İzin silinirken kontrol için
import com.covolt.backend.modules.platform_administration.role_permission_management.dto.PermissionRequestDto;
import com.covolt.backend.modules.platform_administration.role_permission_management.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository; // İzin silinirken rollere atanıp atanmadığını kontrol etmek için

    @Override
    @Transactional
    public Permission createPermission(PermissionRequestDto dto) {
        String permissionName = dto.getName().toUpperCase().replace(" ", "_"); // Standart format
        if (permissionRepository.findByName(permissionName).isPresent()) {
            logger.warn("İzin oluşturma başarısız: '{}' adlı izin zaten mevcut.", permissionName);
            throw new DuplicateResourceException(ErrorCode.RPM_002, permissionName); // RPM_002: "İzin adı zaten kullanımda: %s."
        }

        Permission permission = Permission.builder()
                .name(permissionName)
                .description(dto.getDescription())
                .build();
        Permission savedPermission = permissionRepository.save(permission);
        logger.info("Yeni izin oluşturuldu: ID={}, Adı='{}'", savedPermission.getId(), savedPermission.getName());
        return savedPermission;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionById(UUID id) {
        return permissionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Permission> getPermissionByName(String name) {
        return permissionRepository.findByName(name.toUpperCase().replace(" ", "_"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    @Transactional
    public Permission updatePermission(UUID id, PermissionRequestDto dto) {
        Permission existingPermission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İzin", id));

        String newPermissionName = dto.getName().toUpperCase().replace(" ", "_");
        if (!existingPermission.getName().equals(newPermissionName)) {
            if (permissionRepository.findByName(newPermissionName).isPresent()) {
                logger.warn("İzin güncelleme başarısız: '{}' adlı izin zaten mevcut.", newPermissionName);
                throw new DuplicateResourceException(ErrorCode.RPM_002, newPermissionName);
            }
            existingPermission.setName(newPermissionName);
        }
        existingPermission.setDescription(dto.getDescription());
        Permission updatedPermission = permissionRepository.save(existingPermission);
        logger.info("İzin güncellendi: ID={}, Adı='{}'", updatedPermission.getId(), updatedPermission.getName());
        return updatedPermission;
    }

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        Permission permissionToDelete = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İzin", id));

        // Bu iznin herhangi bir role atanıp atanmadığını kontrol et
        // Bunun için RoleRepository'ye bir metot eklememiz gerekecek:
        // long countByPermissionsContains(Permission permission);
        long roleCountWithPermission = roleRepository.countByPermissionsContains(permissionToDelete);
        if (roleCountWithPermission > 0) {
            logger.warn("İzin silinemedi (ID={}): Bu izin {} role atanmış durumda.", id, roleCountWithPermission);
            // ErrorCode.RPM_004: "İzin silinemez, aktif olarak rollere atanmış."
            // throw new ResourceInUseException(ErrorCode.RPM_004, "izin", permissionToDelete.getName());
            // ResourceInUseException diye yeni bir exception oluşturabiliriz veya IllegalStateException kullanabiliriz.
            throw new IllegalStateException(ErrorCode.RPM_004.formatMessage() + " (İzin: " + permissionToDelete.getName() + ")");
        }

        permissionRepository.delete(permissionToDelete);
        logger.info("İzin silindi: ID={}, Adı='{}'", permissionToDelete.getId(), permissionToDelete.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Permission> findPermissionsByIds(Set<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> permissionsList = permissionRepository.findAllById(permissionIds);
        if (permissionsList.size() != permissionIds.size()) {
            // Hangi ID'lerin bulunamadığını loglayabilir veya hata fırlatabiliriz.
            // RoleServiceImpl'deki fetchPermissionsByIds'a benzer bir mantık.
            logger.warn("findPermissionsByIds: Tüm izin ID'leri bulunamadı. Gelen ID'ler: {}, Bulunanlar: {}",
                    permissionIds, permissionsList.stream().map(Permission::getId).toList());
            // Şimdilik sadece bulunanları dönelim, bu metot genellikle rol oluştururken vs. var olanları doğrulamak için kullanılır.
            // Eğer kesinlikle hepsi bulunmalıysa burada ResourceNotFoundException fırlatılabilir.
        }
        return new HashSet<>(permissionsList);
    }
}
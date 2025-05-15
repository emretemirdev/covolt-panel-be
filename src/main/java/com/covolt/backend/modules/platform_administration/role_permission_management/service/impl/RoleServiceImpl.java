package com.covolt.backend.modules.platform_administration.role_permission_management.service.impl;

import com.covolt.backend.core.exception.ErrorCode;
import com.covolt.backend.core.exception.ResourceNotFoundException; // Genel bir exception (oluşturulmalı)
import com.covolt.backend.core.exception.DuplicateResourceException; // Genel bir exception (oluşturulmalı)
import com.covolt.backend.core.model.Permission;
import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.repository.PermissionRepository;
import com.covolt.backend.core.repository.RoleRepository;
import com.covolt.backend.core.repository.UserRepository; // Rol silinirken kontrol için
import com.covolt.backend.modules.platform_administration.role_permission_management.dto.RoleRequestDto;
import com.covolt.backend.modules.platform_administration.role_permission_management.service.RoleService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository; // Rol silinirken kullanıcı kontrolü için

    private static final String ROLE_NOT_FOUND_MSG = "ID'si %s olan rol bulunamadı.";
    private static final String PERMISSION_NOT_FOUND_MSG = "ID'si %s olan izin bulunamadı.";


    @Override
    @Transactional
    public Role createRole(RoleRequestDto roleDto) {
        // Rol adının benzersizliğini kontrol et (ROLE_ prefix'i eklenmiş haliyle)
        String roleNameWithPrefix = ensureRolePrefix(roleDto.getName());
        if (roleRepository.findByName(roleNameWithPrefix).isPresent()) {
            logger.warn("Rol oluşturma başarısız: '{}' adlı rol zaten mevcut.", roleNameWithPrefix);
            throw new DuplicateResourceException(ErrorCode.RPM_001, roleNameWithPrefix); // ErrorCode ve argüman
        }
        Role newRole = Role.builder()
                .name(roleNameWithPrefix)
                .description(roleDto.getDescription())
                .permissions(new HashSet<>()) // Başlangıçta izinleri boş
                .build();

        // Eğer DTO'da permissionId'ler geldiyse, bunları bul ve role ata
        if (roleDto.getPermissionIds() != null && !roleDto.getPermissionIds().isEmpty()) {
            Set<Permission> permissionsToAssign = fetchPermissionsByIds(roleDto.getPermissionIds());
            newRole.setPermissions(permissionsToAssign);
        }

        Role savedRole = roleRepository.save(newRole);
        logger.info("Yeni rol oluşturuldu: ID={}, Adı='{}'", savedRole.getId(), savedRole.getName());
        return savedRole;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> getRoleById(UUID id) {
        return roleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> getRoleByName(String name) {
        // İsimle ararken de prefix'i dikkate alalım
        return roleRepository.findByName(ensureRolePrefix(name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public Role updateRole(UUID roleId, RoleRequestDto roleDto) {
        Role existingRole = roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    logger.warn(String.format(ROLE_NOT_FOUND_MSG, roleId));
                    return new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleId));
                });

        // Rol adını güncelliyorsak ve yeni ad farklıysa, benzersizlik kontrolü yap
        String newRoleNameWithPrefix = ensureRolePrefix(roleDto.getName());
        if (!existingRole.getName().equals(newRoleNameWithPrefix)) {
            if (roleRepository.findByName(newRoleNameWithPrefix).isPresent()) {
                logger.warn("Rol güncelleme başarısız: '{}' adlı rol zaten mevcut.", newRoleNameWithPrefix);
                throw new DuplicateResourceException(ErrorCode.RPM_001, newRoleNameWithPrefix); // ErrorCode ve argüman
            }
            existingRole.setName(newRoleNameWithPrefix);
        }

        existingRole.setDescription(roleDto.getDescription());

        // İzinleri güncelle: Gelen permissionId'lere göre izin setini tamamen yeniden oluştur.
        // Bu, mevcut izinleri silip yenilerini ekler. Daha granüler (sadece ekle/çıkar) da yapılabilir.
        if (roleDto.getPermissionIds() != null) { // null ise izinleri değiştirme
            Set<Permission> permissionsToAssign = fetchPermissionsByIds(roleDto.getPermissionIds());
            existingRole.setPermissions(permissionsToAssign);
        }

        Role updatedRole = roleRepository.save(existingRole);
        logger.info("Rol güncellendi: ID={}, Adı='{}'", updatedRole.getId(), updatedRole.getName());
        return updatedRole;
    }

    @Override
    @Transactional
    public void deleteRole(UUID roleId) {
        Role roleToDelete = roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    logger.warn(String.format(ROLE_NOT_FOUND_MSG, roleId));
                    return new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleId));
                });

        // Bu role sahip kullanıcı var mı kontrol et?
        // Bu kontrol karmaşık olabilir ve performansı etkileyebilir.
        // Şimdilik basit bir kontrol yapalım veya bu kontrolü es geçip DB constraint'ine güvenelim (eğer varsa).
        // VEYA kullanıcıların rollerini null'a çekmek yerine, bu rolü silmeden önce
        // manuel olarak kullanıcıların rollerinin değiştirilmesi gerektiğini belirten bir kural koyabiliriz.
        long userCountWithRole = userRepository.countByRolesContains(roleToDelete);
        if (userCountWithRole > 0) {
            logger.warn("Rol silinemedi (ID={}): Bu role atanmış {} kullanıcı bulunuyor.", roleId, userCountWithRole);
            // Özel bir exception fırlatılabilir: RoleInUseException
            throw new IllegalStateException("Bu rol silinemez çünkü hala kullanıcılara atanmış durumda.");
        }

        // Eğer rolün User entity'sindeki `roles` setiyle olan ManyToMany ilişkisi Role tarafından yönetiliyorsa
        // (yani Role'de `mappedBy` yoksa), silmeden önce bu ilişkileri temizlemek gerekebilir.
        // Ancak bizim User entity'mizdeki `roles` alanı `mappedBy` olmadığı için (JoinTable User'da)
        // Role silindiğinde `user_roles` tablosundaki ilgili kayıtlar da silinmeli (Hibernate halleder).
        // `role_permissions` tablosu için de aynı durum geçerli (JoinTable Role'de).

        roleRepository.delete(roleToDelete);
        logger.info("Rol silindi: ID={}, Adı='{}'", roleToDelete.getId(), roleToDelete.getName());
    }

    @Override
    @Transactional
    public Role assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleId)));

        if (permissionIds == null || permissionIds.isEmpty()) {
            logger.warn("İzin atama işlemi için permissionId listesi boş veya null. Rol ID: {}", roleId);
            // Hata fırlatmak yerine rolü olduğu gibi döndürebiliriz veya boş işlem yapabiliriz.
            return role;
        }

        Set<Permission> permissionsToAssign = fetchPermissionsByIds(permissionIds);

        // Mevcut izinlere yenilerini ekle (Set özelliği sayesinde duplicate olmaz)
        boolean updated = role.getPermissions().addAll(permissionsToAssign);

        if (updated) {
            Role savedRole = roleRepository.save(role);
            logger.info("{} adet izin role (ID={}) atandı.", permissionIds.size(), roleId);
            return savedRole;
        }
        return role; // Değişiklik olmadıysa
    }

    @Override
    @Transactional
    public Role removePermissionsFromRole(UUID roleId, Set<UUID> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleId)));

        if (permissionIds == null || permissionIds.isEmpty()) {
            logger.warn("İzin kaldırma işlemi için permissionId listesi boş veya null. Rol ID: {}", roleId);
            return role;
        }

        // Veritabanından bu ID'lere sahip Permission nesnelerini çekmeye gerek yok,
        // direkt Role'ün Permission setinden ID'ye göre filtreleyerek kaldırabiliriz.
        // Ancak, gelen permissionId'lerin gerçekten var olup olmadığını kontrol etmek daha güvenli olabilir.
        // Şimdilik direkt Role'deki Set üzerinden removeIf ile ID bazlı kaldıralım.
        // Bu yaklaşım, Role entity'sinin Permission setinin EAGER fetch edilmesine dayanır.
        boolean removed = role.getPermissions().removeIf(permission -> permissionIds.contains(permission.getId()));

        if (removed) {
            Role savedRole = roleRepository.save(role);
            logger.info("{} adet izin rolden (ID={}) kaldırıldı.", permissionIds.size(), roleId); // Kaldırılan gerçek sayıyı bulmak daha iyi olur.
            return savedRole;
        }
        return role;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Role> findRolesByIds(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> rolesList = roleRepository.findAllById(roleIds);
        return new HashSet<>(rolesList);
    }

    // Yardımcı metot: Gelen permission ID'leri ile Permission nesnelerini DB'den çeker.
    private Set<Permission> fetchPermissionsByIds(Set<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> foundPermissions = permissionRepository.findAllById(permissionIds);
        if (foundPermissions.size() != permissionIds.size()) {
            // Bazı izin ID'leri bulunamadı, hangileri olduğunu bulup hata mesajına ekleyebiliriz.
            Set<UUID> foundIds = foundPermissions.stream().map(Permission::getId).collect(Collectors.toSet());
            Set<UUID> notFoundIds = new HashSet<>(permissionIds);
            notFoundIds.removeAll(foundIds);
            logger.warn("Bazı izin ID'leri bulunamadı: {}", notFoundIds);
            throw new ResourceNotFoundException("Belirtilen izin ID'lerinden bazıları bulunamadı: " + notFoundIds);
        }
        return new HashSet<>(foundPermissions);
    }

    // Yardımcı metot: Rol adının "ROLE_" ile başlamasını sağlar.
    private String ensureRolePrefix(String roleName) {
        if (roleName == null) return null;
        roleName = roleName.trim().toUpperCase().replace(" ", "_");
        if (!roleName.startsWith("ROLE_")) {
            return "ROLE_" + roleName;
        }
        return roleName;
    }
}
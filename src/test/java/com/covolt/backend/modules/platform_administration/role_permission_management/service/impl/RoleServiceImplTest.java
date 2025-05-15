package com.covolt.backend.modules.platform_administration.role_permission_management.service.impl;

import com.covolt.backend.core.exception.DuplicateResourceException;
import com.covolt.backend.core.exception.ResourceNotFoundException;
import com.covolt.backend.core.model.Permission;
import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.repository.PermissionRepository;
import com.covolt.backend.core.repository.RoleRepository;
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.modules.platform_administration.role_permission_management.dto.RoleRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections; // Collections importu eklendi
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl Testleri") // Test sınıfı için genel bir başlık
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private RoleRequestDto roleRequestDto;
    private Role role;
    private Permission permission1, permission2;
    private UUID roleId;
    private UUID permissionId1, permissionId2;
    private String roleNameWithPrefix;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        permissionId1 = UUID.randomUUID();
        permissionId2 = UUID.randomUUID();
        roleNameWithPrefix = "ROLE_TEST_ROLE"; // ensureRolePrefix sonrası beklenen ad

        permission1 = Permission.builder().id(permissionId1).name("PERMISSION_ONE").build();
        permission2 = Permission.builder().id(permissionId2).name("PERMISSION_TWO").build();

        roleRequestDto = RoleRequestDto.builder()
                .name("TEST_ROLE") // ensureRolePrefix öncesi ad
                .description("A test role")
                .permissionIds(Set.of(permissionId1))
                .build();

        role = Role.builder()
                .id(roleId)
                .name(roleNameWithPrefix)
                .description("A test role")
                .permissions(new HashSet<>(Set.of(permission1)))
                .build();
    }

    // --- createRole Testleri (Mevcut olanlar iyi, sadece gruplayabiliriz) ---
    @Nested
    @DisplayName("createRole Metodu Testleri")
    class CreateRoleTests {
        @Test
        @DisplayName("Benzersiz isim ve geçerli izinlerle rol başarıyla oluşturulmalı")
        void createRole_whenNameIsUniqueAndPermissionsExist_shouldSaveAndReturnRole() {
            when(roleRepository.findByName(roleNameWithPrefix)).thenReturn(Optional.empty());
            when(permissionRepository.findAllById(Set.of(permissionId1))).thenReturn(List.of(permission1));
            when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
                Role roleToSave = invocation.getArgument(0);
                roleToSave.setId(roleId); // ID'yi mock'ta set et
                return roleToSave;
            });


            Role createdRole = roleService.createRole(roleRequestDto);

            assertNotNull(createdRole);
            assertEquals(roleNameWithPrefix, createdRole.getName());
            assertEquals(roleRequestDto.getDescription(), createdRole.getDescription());
            assertTrue(createdRole.getPermissions().contains(permission1));
            assertEquals(1, createdRole.getPermissions().size());

            verify(roleRepository).findByName(roleNameWithPrefix);
            verify(permissionRepository).findAllById(Set.of(permissionId1));
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("Var olan bir isimle rol oluşturulmaya çalışıldığında DuplicateResourceException fırlatmalı")
        void createRole_whenNameIsNotUnique_shouldThrowDuplicateResourceException() {
            when(roleRepository.findByName(roleNameWithPrefix)).thenReturn(Optional.of(role));

            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
                roleService.createRole(roleRequestDto);
            });
            assertEquals("'" + roleNameWithPrefix + "' adlı rol zaten mevcut.", exception.getMessage());
            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("Geçersiz izin ID'si ile rol oluşturulmaya çalışıldığında ResourceNotFoundException fırlatmalı")
        void createRole_whenPermissionIdNotFound_shouldThrowResourceNotFoundException() {
            UUID nonExistentPermissionId = UUID.randomUUID();
            RoleRequestDto dtoWithInvalidPermission = RoleRequestDto.builder()
                    .name("NEW_ROLE")
                    .permissionIds(Set.of(permissionId1, nonExistentPermissionId))
                    .build();
            when(roleRepository.findByName("ROLE_NEW_ROLE")).thenReturn(Optional.empty());
            when(permissionRepository.findAllById(dtoWithInvalidPermission.getPermissionIds()))
                    .thenReturn(List.of(permission1)); // Sadece var olanı döndür

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                roleService.createRole(dtoWithInvalidPermission);
            });
            assertTrue(exception.getMessage().contains(nonExistentPermissionId.toString()));
            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("İzin ID'leri olmadan rol başarıyla oluşturulmalı")
        void createRole_withoutPermissionIds_shouldCreateRoleWithEmptyPermissions() {
            RoleRequestDto dtoWithoutPermissions = RoleRequestDto.builder()
                    .name("SIMPLE_ROLE")
                    .description("A simple role")
                    .build();
            Role expectedRole = Role.builder()
                    .id(UUID.randomUUID())
                    .name("ROLE_SIMPLE_ROLE")
                    .description("A simple role")
                    .permissions(new HashSet<>())
                    .build();

            when(roleRepository.findByName("ROLE_SIMPLE_ROLE")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenReturn(expectedRole);

            Role createdRole = roleService.createRole(dtoWithoutPermissions);

            assertNotNull(createdRole);
            assertEquals("ROLE_SIMPLE_ROLE", createdRole.getName());
            assertTrue(createdRole.getPermissions().isEmpty());
            verify(permissionRepository, never()).findAllById(any());
            verify(roleRepository).save(any(Role.class));
        }
    }


    // --- updateRole Testleri ---
    @Nested
    @DisplayName("updateRole Metodu Testleri")
    class UpdateRoleTests {
        @Test
        @DisplayName("Var olan bir rol güncellendiğinde değişiklikler kaydedilmeli")
        void updateRole_whenRoleExists_shouldUpdateAndReturnRole() {
            RoleRequestDto updateDto = RoleRequestDto.builder()
                    .name("UPDATED_TEST_ROLE")
                    .description("Updated description")
                    .permissionIds(Set.of(permissionId1, permissionId2)) // İzinleri değiştir
                    .build();
            String updatedRoleNameWithPrefix = "ROLE_UPDATED_TEST_ROLE";

            Role existingRole = Role.builder()
                    .id(roleId)
                    .name(roleNameWithPrefix) // Eski isim
                    .description("A test role")
                    .permissions(new HashSet<>(Set.of(permission1))) // Eski izinler
                    .build();

            // ArgumentCaptor ile save metoduna giden Role nesnesini yakalayabiliriz
            ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);


            when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
            when(roleRepository.findByName(updatedRoleNameWithPrefix)).thenReturn(Optional.empty()); // Yeni isim unique
            when(permissionRepository.findAllById(Set.of(permissionId1, permissionId2)))
                    .thenReturn(List.of(permission1, permission2));
            when(roleRepository.save(roleCaptor.capture())).thenAnswer(invocation -> roleCaptor.getValue()); // Yakalanan argümanı döndür


            Role updatedRole = roleService.updateRole(roleId, updateDto);

            assertNotNull(updatedRole);
            assertEquals(updatedRoleNameWithPrefix, updatedRole.getName());
            assertEquals("Updated description", updatedRole.getDescription());
            assertEquals(2, updatedRole.getPermissions().size());
            assertTrue(updatedRole.getPermissions().contains(permission1));
            assertTrue(updatedRole.getPermissions().contains(permission2));

            verify(roleRepository).findById(roleId);
            verify(roleRepository).findByName(updatedRoleNameWithPrefix);
            verify(permissionRepository).findAllById(Set.of(permissionId1, permissionId2));
            verify(roleRepository).save(any(Role.class));

            // Save metoduna giden nesnenin doğruluğunu kontrol et
            Role capturedRole = roleCaptor.getValue();
            assertEquals(updatedRoleNameWithPrefix, capturedRole.getName());
            assertEquals("Updated description", capturedRole.getDescription());
        }

        @Test
        @DisplayName("Var olmayan bir rol güncellenmeye çalışıldığında ResourceNotFoundException fırlatmalı")
        void updateRole_whenRoleDoesNotExist_shouldThrowResourceNotFoundException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                roleService.updateRole(roleId, roleRequestDto);
            });
            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("Rol adı var olan başka bir rolle aynı olacak şekilde güncellenmeye çalışıldığında DuplicateResourceException fırlatmalı")
        void updateRole_toExistingName_shouldThrowDuplicateResourceException() {
            RoleRequestDto updateDto = RoleRequestDto.builder().name("EXISTING_OTHER_ROLE").build();
            String newNameWithPrefix = "ROLE_EXISTING_OTHER_ROLE";

            Role existingRoleToUpdate = Role.builder().id(roleId).name(roleNameWithPrefix).build(); // Güncellenecek rol
            Role otherExistingRole = Role.builder().id(UUID.randomUUID()).name(newNameWithPrefix).build(); // Bu isme sahip başka bir rol

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRoleToUpdate));
            when(roleRepository.findByName(newNameWithPrefix)).thenReturn(Optional.of(otherExistingRole)); // Yeni isim zaten kullanımda

            assertThrows(DuplicateResourceException.class, () -> {
                roleService.updateRole(roleId, updateDto);
            });
            verify(roleRepository, never()).save(any(Role.class));
        }

        @Test
        @DisplayName("Rol güncellenirken izin ID'leri null ise izinler değişmemeli")
        void updateRole_withNullPermissionIds_shouldNotChangePermissions() {
            RoleRequestDto updateDto = RoleRequestDto.builder()
                    .name("TEST_ROLE") // İsim aynı kalsın
                    .description("Updated description")
                    .permissionIds(null) // İzinler null
                    .build();

            // Mevcut rolün bir izni olsun
            Role existingRoleWithPermission = Role.builder()
                    .id(roleId)
                    .name(roleNameWithPrefix)
                    .description("Original Description")
                    .permissions(new HashSet<>(Set.of(permission1)))
                    .build();

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRoleWithPermission));
            // İsim değişmediği için findByName çağrılmayacak
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role updatedRole = roleService.updateRole(roleId, updateDto);

            assertEquals("Updated description", updatedRole.getDescription());
            assertEquals(1, updatedRole.getPermissions().size()); // İzin sayısı değişmedi
            assertTrue(updatedRole.getPermissions().contains(permission1)); // Aynı izin duruyor
            verify(permissionRepository, never()).findAllById(any()); // İzinler aranmadı
        }
    }

    // --- deleteRole Testleri ---
    @Nested
    @DisplayName("deleteRole Metodu Testleri")
    class DeleteRoleTests {
        @Test
        @DisplayName("Kullanımda olmayan bir rol silindiğinde başarılı olmalı")
        void deleteRole_whenRoleNotUsed_shouldDeleteSuccessfully() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(userRepository.countByRolesContains(role)).thenReturn(0L); // Rol kullanımda değil
            doNothing().when(roleRepository).delete(role); // delete void olduğu için doNothing

            assertDoesNotThrow(() -> {
                roleService.deleteRole(roleId);
            });

            verify(roleRepository).delete(role);
        }

        @Test
        @DisplayName("Var olmayan bir rol silinmeye çalışıldığında ResourceNotFoundException fırlatmalı")
        void deleteRole_whenRoleNotFound_shouldThrowResourceNotFoundException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                roleService.deleteRole(roleId);
            });
            verify(userRepository, never()).countByRolesContains(any());
            verify(roleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Kullanımda olan bir rol silinmeye çalışıldığında IllegalStateException fırlatmalı")
        void deleteRole_whenRoleInUse_shouldThrowIllegalStateException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(userRepository.countByRolesContains(role)).thenReturn(1L); // Rol kullanımda

            assertThrows(IllegalStateException.class, () -> {
                roleService.deleteRole(roleId);
            });
            verify(roleRepository, never()).delete(any());
        }
    }

    // --- assignPermissionsToRole Testleri ---
    @Nested
    @DisplayName("assignPermissionsToRole Metodu Testleri")
    class AssignPermissionsTests {
        @Test
        @DisplayName("Var olan role yeni izinler atandığında başarılı olmalı")
        void assignPermissionsToRole_whenRoleAndPermissionsExist_shouldUpdateRole() {
            Role existingRole = Role.builder().id(roleId).name(roleNameWithPrefix).permissions(new HashSet<>()).build(); // Başlangıçta izinsiz
            Set<UUID> newPermissionIds = Set.of(permissionId1, permissionId2);

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
            when(permissionRepository.findAllById(newPermissionIds)).thenReturn(List.of(permission1, permission2));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role updatedRole = roleService.assignPermissionsToRole(roleId, newPermissionIds);

            assertEquals(2, updatedRole.getPermissions().size());
            assertTrue(updatedRole.getPermissions().containsAll(Set.of(permission1, permission2)));
            verify(roleRepository).save(existingRole);
        }

        @Test
        @DisplayName("Var olan role zaten sahip olduğu bir izin tekrar atanmaya çalışıldığında değişiklik olmamalı")
        void assignPermissionsToRole_withExistingPermission_shouldNotDuplicate() {
            // 'role' @BeforeEach'te permission1 ile oluşturulmuştu.
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role)); // role zaten permission1'e sahip
            when(permissionRepository.findAllById(Set.of(permissionId1))).thenReturn(List.of(permission1));
            // save çağrılmayabilir veya çağrılırsa da değişiklik olmamalı.
            // `addAll` zaten duplicate eklemez, bu yüzden `updated` false döner ve save çağrılmaz.

            Role resultRole = roleService.assignPermissionsToRole(roleId, Set.of(permissionId1));

            assertEquals(1, resultRole.getPermissions().size());
            assertTrue(resultRole.getPermissions().contains(permission1));
            verify(roleRepository, never()).save(any(Role.class)); // Çünkü değişiklik olmadı
        }

        @Test
        @DisplayName("Var olmayan bir role izin atanmaya çalışıldığında ResourceNotFoundException fırlatmalı")
        void assignPermissionsToRole_whenRoleNotFound_shouldThrowException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                roleService.assignPermissionsToRole(roleId, Set.of(permissionId1));
            });
        }

        @Test
        @DisplayName("Var olmayan bir izin ID'si ile atama yapılmaya çalışıldığında ResourceNotFoundException fırlatmalı")
        void assignPermissionsToRole_whenPermissionNotFound_shouldThrowException() {
            UUID nonExistentPermissionId = UUID.randomUUID();
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(permissionRepository.findAllById(Set.of(nonExistentPermissionId))).thenReturn(Collections.emptyList()); // İzin bulunamadı

            assertThrows(ResourceNotFoundException.class, () -> {
                roleService.assignPermissionsToRole(roleId, Set.of(nonExistentPermissionId));
            });
        }
    }

    // --- removePermissionsFromRole Testleri ---
    @Nested
    @DisplayName("removePermissionsFromRole Metodu Testleri")
    class RemovePermissionsTests {
        @Test
        @DisplayName("Var olan bir rolden izinler kaldırıldığında başarılı olmalı")
        void removePermissionsFromRole_whenPermissionsExist_shouldUpdateRole() {
            // 'role' @BeforeEach'te permission1 ile oluşturulmuştu.
            // permission2'yi de ekleyelim ki kaldıracak bir şey olsun
            role.getPermissions().add(permission2);

            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role updatedRole = roleService.removePermissionsFromRole(roleId, Set.of(permissionId1)); // permission1'i kaldır

            assertEquals(1, updatedRole.getPermissions().size());
            assertFalse(updatedRole.getPermissions().contains(permission1));
            assertTrue(updatedRole.getPermissions().contains(permission2)); // permission2 duruyor olmalı
            verify(roleRepository).save(role);
        }

        @Test
        @DisplayName("Var olmayan bir rolden izin kaldırılmaya çalışıldığında ResourceNotFoundException fırlatmalı")
        void removePermissionsFromRole_whenRoleNotFound_shouldThrowException() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                roleService.removePermissionsFromRole(roleId, Set.of(permissionId1));
            });
        }

        @Test
        @DisplayName("Rolden olmayan bir izin kaldırılmaya çalışıldığında değişiklik olmamalı")
        void removePermissionsFromRole_withNonExistingPermissionInRole_shouldDoNothing() {
            UUID otherPermissionId = UUID.randomUUID();
            // 'role' @BeforeEach'te sadece permission1'e sahip
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

            Role resultRole = roleService.removePermissionsFromRole(roleId, Set.of(otherPermissionId));

            assertEquals(1, resultRole.getPermissions().size()); // İzin sayısı değişmedi
            assertTrue(resultRole.getPermissions().contains(permission1));
            verify(roleRepository, never()).save(any(Role.class)); // Değişiklik olmadı
        }
    }

    // --- getRoleById, getRoleByName, getAllRoles, findRolesByIds Testleri ---
    // Bu metodlar genellikle repository'ye doğrudan delege ettiği için testleri daha basit olur.
    @Nested
    @DisplayName("Get Role(s) Metodu Testleri")
    class GetRoleTests {
        @Test
        @DisplayName("Geçerli ID ile getRoleById çağrıldığında rol dönmeli")
        void getRoleById_whenRoleExists_shouldReturnRole() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
            Optional<Role> foundRole = roleService.getRoleById(roleId);
            assertTrue(foundRole.isPresent());
            assertEquals(role, foundRole.get());
        }

        @Test
        @DisplayName("Geçersiz ID ile getRoleById çağrıldığında boş Optional dönmeli")
        void getRoleById_whenRoleNotExists_shouldReturnEmpty() {
            when(roleRepository.findById(roleId)).thenReturn(Optional.empty());
            Optional<Role> foundRole = roleService.getRoleById(roleId);
            assertFalse(foundRole.isPresent());
        }

        @Test
        @DisplayName("Geçerli isimle getRoleByName çağrıldığında rol dönmeli")
        void getRoleByName_whenRoleExists_shouldReturnRole() {
            String rawName = "TEST_ROLE"; // ensureRolePrefix öncesi
            when(roleRepository.findByName(roleNameWithPrefix)).thenReturn(Optional.of(role));
            Optional<Role> foundRole = roleService.getRoleByName(rawName);
            assertTrue(foundRole.isPresent());
            assertEquals(role, foundRole.get());
            verify(roleRepository).findByName(roleNameWithPrefix); // ensureRolePrefix'in çalıştığını da doğrular
        }

        @Test
        @DisplayName("Tüm roller getAllRoles ile getirilmeli")
        void getAllRoles_shouldReturnAllRoles() {
            List<Role> roles = List.of(role, Role.builder().id(UUID.randomUUID()).name("ROLE_ANOTHER").build());
            when(roleRepository.findAll()).thenReturn(roles);

            List<Role> result = roleService.getAllRoles();

            assertEquals(2, result.size());
            assertTrue(result.containsAll(roles));
        }

        @Test
        @DisplayName("Verilen ID'lerle findRolesByIds çağrıldığında ilgili roller dönmeli")
        void findRolesByIds_withValidIds_shouldReturnRoles() {
            Role role2 = Role.builder().id(UUID.randomUUID()).name("ROLE_ANOTHER").build();
            Set<UUID> idsToFind = Set.of(role.getId(), role2.getId());
            when(roleRepository.findAllById(idsToFind)).thenReturn(List.of(role, role2));

            Set<Role> foundRoles = roleService.findRolesByIds(idsToFind);

            assertEquals(2, foundRoles.size());
            assertTrue(foundRoles.contains(role));
            assertTrue(foundRoles.contains(role2));
        }

        @Test
        @DisplayName("Boş ID seti ile findRolesByIds çağrıldığında boş set dönmeli")
        void findRolesByIds_withEmptySet_shouldReturnEmptySet() {
            Set<Role> foundRoles = roleService.findRolesByIds(Collections.emptySet());
            assertTrue(foundRoles.isEmpty());
            verify(roleRepository, never()).findAllById(any());
        }
    }
}
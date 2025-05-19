package com.covolt.backend.core.repository; // Paket yolunu kontrol et

import com.covolt.backend.core.model.Permission;
import com.covolt.backend.core.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID; // UUID importu
import java.util.List;
import java.util.Collection;
import com.covolt.backend.core.model.User;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);

    long countByPermissionsContains(Permission permission);
    
    // Açıklama bazlı rol arama
    Optional<Role> findByDescription(String description);
    
    // Belirli bir izne sahip rolleri bulma
    List<Role> findByPermissionsContains(Permission permission);
    
    // İzin listesinden herhangi birine sahip rolleri bulma
    List<Role> findByPermissionsIn(Collection<Permission> permissions);
    
    // Rol adı içeren (case-insensitive) rolleri bulma
    List<Role> findByNameContainingIgnoreCase(String namePattern);
    
    // Belirli bir kullanıcının sahip olduğu rolleri bulma
    List<Role> findByUsersContains(User user);
    
    // Belirli bir izne sahip olmayan rolleri bulma
    List<Role> findByPermissionsNotContains(Permission permission);
}
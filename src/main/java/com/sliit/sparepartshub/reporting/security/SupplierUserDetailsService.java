package com.sliit.sparepartshub.reporting.security;

import com.sliit.sparepartshub.entity.Supplier;
import com.sliit.sparepartshub.reporting.repository.SupplierRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SupplierUserDetailsService implements UserDetailsService {

    private final SupplierRepository supplierRepository;

    public SupplierUserDetailsService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Supplier supplier = supplierRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No supplier account found for email: " + email));
        return new CustomSupplierPrincipal(supplier);
    }
}

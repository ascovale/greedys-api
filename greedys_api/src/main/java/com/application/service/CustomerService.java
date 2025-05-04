package com.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.customer.AllergyDAO;
import com.application.persistence.dao.customer.CustomerDAO;
import com.application.persistence.dao.customer.PrivilegeDAO;
import com.application.persistence.dao.customer.RoleDAO;
import com.application.persistence.model.Image;
import com.application.persistence.model.customer.Allergy;
import com.application.persistence.model.customer.Customer;
import com.application.persistence.model.customer.Privilege;
import com.application.persistence.model.customer.Role;
import com.application.web.dto.get.AllergyDTO;
import com.application.web.dto.get.CustomerDTO;
import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.error.UserAlreadyExistException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class CustomerService {

	private final CustomerDAO customerDAO;
	private final RoleDAO roleRepository;
	private final EntityManager entityManager;
	private final AllergyDAO allergyDAO;
	private final PrivilegeDAO privilegeDAO;

	public CustomerService(CustomerDAO customerDAO,
			RoleDAO roleRepository,
			EntityManager entityManager,
			AllergyDAO allergyDAO,
			PrivilegeDAO privilegeDAO) {
		this.customerDAO = customerDAO;
		this.roleRepository = roleRepository;
		this.entityManager = entityManager;
		this.privilegeDAO = privilegeDAO;
		this.allergyDAO = allergyDAO;
	}

	public CustomerDTO findById(long id) {
		Customer customer = customerDAO.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		return new CustomerDTO(customer);
	}

	public Customer findCustomerByEmail(final String email) {
		return customerDAO.findByEmail(email);
	}

	public Optional<Customer> getCustomerByID(final long id) {
		return customerDAO.findById(id);
	}

	private boolean emailExists(final String email) {
		return customerDAO.findByEmail(email) != null;
	}

	public List<Customer> findAll() {
		return customerDAO.findAll();
	}

	public void addImage(String email, Image image) {
		Customer customer = customerDAO.findByEmail(email);
		customerDAO.save(customer);
	}

	public Customer getReference(Long customerId) {
		return entityManager.getReference(Customer.class, customerId);
	}

	public void deleteCustomerById(Long id) {
		Customer customer = customerDAO.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customerDAO.delete(customer);
	}

	public Customer updateCustomer(Long id, NewCustomerDTO customerDto) {
		Customer customer = customerDAO.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		if (customerDto.getFirstName() != null) {
			customer.setName(customerDto.getFirstName());
		}
		if (customerDto.getLastName() != null) {
			customer.setSurname(customerDto.getLastName());
		}
		if (customerDto.getEmail() != null && !customerDto.getEmail().equals(customer.getEmail())) {
			if (emailExists(customerDto.getEmail())) {
				throw new UserAlreadyExistException(
						"There is an account with that email address: " + customerDto.getEmail());
			}
			customer.setEmail(customerDto.getEmail());
		}
		return customerDAO.save(customer);
	}

	@Transactional
	public void addAllergy(Long idAllergy) {
		Customer customer = customerDAO.findById(getCurrentCustomer().getId())
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		Hibernate.initialize(customer.getAllergies());
		customer.getAllergies().add(allergy);
		customerDAO.save(customer);
	}

	@Transactional
	public void removeAllergy(Long idAllergy) {
		Customer customer = customerDAO.findById(getCurrentCustomer().getId())
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		customer.getAllergies().remove(allergy);
		customerDAO.save(customer);
	}

	private Customer getCurrentCustomer() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof Customer) {
			return ((Customer) principal);
		} else {
			System.out.println("Questo non dovrebbe succedere");
			return null;
		}
	}

	@Transactional
	public void enableCustomer(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.ENABLED);
		customerDAO.save(customer);
	}

	public void blockCustomer(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.DISABLED);
		customerDAO.save(customer);
	}

	public void reportRestaurantAbuse(Long restaurantId) {
		// TODO in futuro decidere cosa farà anche solo mandare una mail
		// L'utente segnala qualche tipo di abuso nella recensione o altro
	}

	@Transactional
	public List<AllergyDTO> getAllergies(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		return customer.getAllergies().stream()
				.map(AllergyDTO::new)
				.collect(Collectors.toList());
	}

	public Page<CustomerDTO> findAll(PageRequest pageable) {
		return customerDAO.findAll(pageable).map(CustomerDTO::new);
	}

	public void addRoleToCustomer(Long customerId, String role) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Hibernate.initialize(customer.getRoles());
		customer.getRoles().add(roleRepository.findByName(role));
		customerDAO.save(customer);
	}

	public void removeRoleFromCustomer(Long customerId, String role) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.getRoles().remove(roleRepository.findByName(role));
		customerDAO.save(customer);
	}

	public void addPrivilegeToRole(String roleName, String privilegeName) {
		Role role = roleRepository.findByName(roleName);
		if (role == null) {
			throw new EntityNotFoundException("Role not found");
		}
		Privilege privilege = privilegeDAO.findByName(privilegeName);
		if (privilege == null) {
			throw new EntityNotFoundException("Permission not found");
		}
		Hibernate.initialize(role.getPrivileges());
		role.getPrivileges().add(privilege);
		roleRepository.save(role);
	}

	public void updateCustomerStatus(Long customerId, Customer.Status newStatus) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new IllegalArgumentException("Customer not found"));

		customer.setStatus(newStatus);
		customerDAO.save(customer);
	}

	public void save(Customer customer) {
		customerDAO.save(customer);
	}

	public void updateFirstName(Long customerId, String firstName) {
		Customer customer = findCustomerById(customerId);
		if (customer == null) {
			throw new IllegalStateException("Customer not found");
		}
		customer.setName(firstName);
		saveCustomer(customer);
	}

	public void updateLastName(Long customerId, String lastName) {
		Customer customer = findCustomerById(customerId);
		if (customer == null) {
			throw new IllegalStateException("Customer not found");
		}
		customer.setSurname(lastName);
		saveCustomer(customer);
	}

	public void updateEmail(Long customerId, String email) {
		Customer customer = findCustomerById(customerId);
		if (customer == null) {
			throw new IllegalStateException("Customer not found");
		}
		customer.setEmail(email);
		saveCustomer(customer);
	}

	private Customer findCustomerById(Long customerId) {
		return null;
	}

	private void saveCustomer(Customer customer) {
	}

	public void markCustomerHasDeleted(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.AUTO_DELETE);
		customerDAO.save(customer);
	}

	public Object adminLoginToCustomer(Long customerId, HttpServletRequest request) {
		throw new UnsupportedOperationException("Unimplemented method 'adminLoginToCustomer'");
	}

	private List<? extends GrantedAuthority> getSwitchUserAuthoritiesAdmin() {
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("PRIVILEGE_ADMIN_SWITCH_TO_RESTAURANT_USER"));
		return authorities;
	}
}

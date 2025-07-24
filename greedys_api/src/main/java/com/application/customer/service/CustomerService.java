package com.application.customer.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.common.persistence.model.Image;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.get.AllergyDTO;
import com.application.common.web.dto.get.CustomerDTO;
import com.application.common.web.dto.get.CustomerStatisticsDTO;
import com.application.common.web.error.UserAlreadyExistException;
import com.application.customer.persistence.dao.AllergyDAO;
import com.application.customer.persistence.dao.CustomerDAO;
import com.application.customer.persistence.dao.PrivilegeDAO;
import com.application.customer.persistence.dao.ReservationDAO;
import com.application.customer.persistence.dao.RoleDAO;
import com.application.customer.persistence.model.Allergy;
import com.application.customer.persistence.model.Customer;
import com.application.customer.persistence.model.Privilege;
import com.application.customer.persistence.model.Role;
import com.application.customer.web.post.NewCustomerDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerDAO customerDAO;
	private final RoleDAO roleRepository;
	private final EntityManager entityManager;
	private final AllergyDAO allergyDAO;
	private final PrivilegeDAO privilegeDAO;
	private final ReservationDAO reservationDAO;

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
	public void addAllergyToCustomer(Long idAllergy) {
		Customer customer = customerDAO.findById(getCurrentCustomer().getId())
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		Hibernate.initialize(customer.getAllergies());
		customer.getAllergies().add(allergy);
		customerDAO.save(customer);
	}

	@Transactional
	public void removeAllergyToCustomer(Long idAllergy) {
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

	public void removePrivilegeFromRole(String roleName, String privilegeName) {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new EntityNotFoundException("Role not found");
        }
        Privilege privilege = privilegeDAO.findByName(privilegeName);
        if (privilege == null) {
            throw new EntityNotFoundException("Permission not found");
        }
        Hibernate.initialize(role.getPrivileges());
        role.getPrivileges().remove(privilege);
        roleRepository.save(role);
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

	public void updatePhone(Long customerId, String newPhone) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setPhoneNumber(newPhone);
		customerDAO.save(customer);
	}

	public void updateDateOfBirth(Long customerId, Date newDateOfBirth) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setDateOfBirth(newDateOfBirth);
		customerDAO.save(customer);
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

    public CustomerStatisticsDTO getCustomerStatistics(Long customerId) {
        // Verify customer exists
        customerDAO.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        
        // Get all statistics counts
        long totalReservations = reservationDAO.countByCustomer(customerId);
		long acceptedReservations = reservationDAO.countByCustomerAndStatus(customerId, Reservation.Status.ACCEPTED);
		long pendingReservations = reservationDAO.countByCustomerAndStatus(customerId, Reservation.Status.NOT_ACCEPTED);
		long rejectedReservations = reservationDAO.countByCustomerAndStatus(customerId, Reservation.Status.REJECTED);
		long noShowReservations = reservationDAO.countByCustomerAndStatus(customerId, Reservation.Status.NO_SHOW);
		long seatedReservations = reservationDAO.countByCustomerAndStatus(customerId, Reservation.Status.SEATED);
		long deletedReservations = reservationDAO.countByCustomerAndStatus(customerId, Reservation.Status.DELETED);

		
        return new CustomerStatisticsDTO(
            totalReservations,
            acceptedReservations,
            pendingReservations,
            rejectedReservations,
            noShowReservations,
            seatedReservations,
            deletedReservations
        );
    }

	public Page<AllergyDTO> getPaginatedAllergies(int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size);
		Customer customer = customerDAO.findById(getCurrentCustomer().getId())
			.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Page<Allergy> allergiesPage = customerDAO.findCustomerAllergies(customer, pageRequest);
		return allergiesPage.map(AllergyDTO::new);
	}



}

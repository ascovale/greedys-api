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

import com.application.common.persistence.mapper.CustomerDTOMapper;
import com.application.common.persistence.model.Image;
import com.application.common.persistence.model.reservation.Reservation;
import com.application.common.web.dto.customer.AllergyDTO;
import com.application.common.web.dto.customer.CustomerDTO;
import com.application.common.web.dto.customer.CustomerStatisticsDTO;
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
import com.application.customer.web.dto.customer.NewCustomerDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerDAO customerDAO;
	private final RoleDAO roleRepository;
	private final AllergyDAO allergyDAO;
	private final PrivilegeDAO privilegeDAO;
	private final ReservationDAO reservationDAO;
	private final CustomerDTOMapper customerDTOMapper;

	public CustomerDTO findById(long id) {
		Customer customer = customerDAO.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		return customerDTOMapper.toDTO(customer);
	}

	public CustomerDTO findCustomerByEmail(final String email) {
		return customerDTOMapper.toDTO(customerDAO.findByEmail(email));
	}

	public Optional<CustomerDTO> getCustomerByID(final long id) {
		return customerDAO.findById(id).map(customerDTOMapper::toDTO);
	}

	private boolean emailExists(final String email) {
		return customerDAO.findByEmail(email) != null;
	}

	public void addImage(String email, Image image) {
		Customer customer = customerDAO.findByEmail(email);
		customerDAO.save(customer);
	}

	public CustomerDTO getReference(Long customerId) {
		return customerDTOMapper.toDTO(customerDAO.getReferenceById(customerId));
	}

	public void deleteCustomerById(Long id) {
		Customer customer = customerDAO.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customerDAO.delete(customer);
	}

	public CustomerDTO updateCustomer(Long id, NewCustomerDTO customerDto) {
		Customer customer = customerDAO.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		
		customerDTOMapper.updateEntityFromDTO(customerDto, customer);
		
		// Controllo email duplicata se è stata cambiata
		if (customerDto.getEmail() != null && !customerDto.getEmail().equals(customer.getEmail())) {
			if (emailExists(customerDto.getEmail())) {
				throw new UserAlreadyExistException(
						"There is an account with that email address: " + customerDto.getEmail());
			}
		}

		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

	public CustomerDTO createCustomer(NewCustomerDTO newCustomerDTO) {
		// Verifica che l'email non esista già
		if (emailExists(newCustomerDTO.getEmail())) {
			throw new UserAlreadyExistException(
					"There is an account with that email address: " + newCustomerDTO.getEmail());
		}
		Customer customer = customerDTOMapper.toEntity(newCustomerDTO);
		customer.setStatus(Customer.Status.ENABLED);
		// La password sarà gestita separatamente dal service di autenticazione
		
		Customer savedCustomer = customerDAO.save(customer);
		return customerDTOMapper.toDTO(savedCustomer);
	}

	public void addAllergyToCustomer(Long idAllergy) {
		Customer customer = customerDAO.findById(getCurrentCustomer().getId())
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Allergy allergy = allergyDAO.findById(idAllergy)
				.orElseThrow(() -> new EntityNotFoundException("Allergy not found"));
		Hibernate.initialize(customer.getAllergies());
		customer.getAllergies().add(allergy);
		customerDAO.save(customer);
	}

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

	public CustomerDTO enableCustomer(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.ENABLED);
		customerDAO.save(customer);
		return customerDTOMapper.toDTO(customer);
	}

	public CustomerDTO blockCustomer(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.DISABLED);
		customerDAO.save(customer);
		return customerDTOMapper.toDTO(customer);
	}

	public void reportRestaurantAbuse(Long restaurantId) {
		// TODO in futuro decidere cosa farà anche solo mandare una mail
		// L'utente segnala qualche tipo di abuso nella recensione o altro
	}

	public List<AllergyDTO> getAllergies(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		return customer.getAllergies().stream()
				.map(AllergyDTO::new)
				.collect(Collectors.toList());
	}

	public Page<CustomerDTO> findAll(PageRequest pageable) {
		return customerDAO.findAll(pageable).map(customerDTOMapper::toDTO);
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



	public CustomerDTO save(Customer customer) {
		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

	public CustomerDTO updateFirstName(Long customerId, String firstName) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setName(firstName);
		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

	public CustomerDTO updateLastName(Long customerId, String lastName) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setSurname(lastName);
		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

	public CustomerDTO updateEmail(Long customerId, String email) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setEmail(email);
		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

	public CustomerDTO updatePhone(Long customerId, String newPhone) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setPhoneNumber(newPhone);
		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

	public CustomerDTO updateDateOfBirth(Long customerId, Date newDateOfBirth) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setDateOfBirth(newDateOfBirth);
		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

	public CustomerDTO markCustomerHasDeleted(Long customerId) {
		Customer customer = customerDAO.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		customer.setStatus(Customer.Status.AUTO_DELETE);
		return customerDTOMapper.toDTO(customerDAO.save(customer));
	}

    public CustomerStatisticsDTO getCustomerStatistics(Long customerId) {
        customerDAO.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        
        long totalReservations = reservationDAO.countByCustomerId(customerId);
        long acceptedReservations = reservationDAO.countByCustomerIdAndStatus(customerId, Reservation.Status.ACCEPTED);
        long pendingReservations = reservationDAO.countByCustomerIdAndStatus(customerId, Reservation.Status.NOT_ACCEPTED);
        long rejectedReservations = reservationDAO.countByCustomerIdAndStatus(customerId, Reservation.Status.REJECTED);
        long noShowReservations = reservationDAO.countByCustomerIdAndStatus(customerId, Reservation.Status.NO_SHOW);
        long seatedReservations = reservationDAO.countByCustomerIdAndStatus(customerId, Reservation.Status.SEATED);
        long deletedReservations = reservationDAO.countByCustomerIdAndStatus(customerId, Reservation.Status.DELETED);
        
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

CompanyUserController sistemare Dto inserita ma bisogna capire come caricare i dati pochi per volta
fare la query in menuDAO findByRestaurantId

rimuovere il controllo doppio dal login   ---> OK
rimuovere il controllo della location   ---> lasciato per ora potrebbe servire
rimuovere enable tramite mail temporaneo 
rimuovere quindi anche resend

FARE: ( https://github.com/eugenp/tutorials/tree/master/spring-security-mvc-boot )
tutto in json restful web service
JAX-RS SecurityContext instance al posto di acl
paginazione per le immagini della galleria


--- creare database necessario
DATABASE  -> aggiungere vincoli quindi trigger ma alla fine
AGGIUNGERE LOGIN CON ACL	---> fatto in parte quindi devo aggiungere
impostare connessione https  ---> https://www.baeldung.com/spring-channel-security-https
errorscontroller per ridirigere ogni errore ---> https://grails-plugins.github.io/grails-spring-security-acl/v3/index.html
AGGIORNARE ENTITA mettere i 
2 LOGIN da agency e da restaurant
//automatic query
List<Test> findByUsers_UserName(String userName)
dentro Test List<Users>
/**
@Entity
public class User {
    private Long id;
    private String userName;
}

@Entity
public class Test {
    private Long id;

    @ManyToMany
    private Set<User> users;
}
@Query("select t from Test t join User u where u.username = :username")
List<Test> findAllByUsername(@Param("username")String username);


ALTRO METODO List<Person> findByAddress_ZipCode(String zipCode);
@Repository
public interface PersonRepository extends CrudRepository<Person, Long> {

    @Query( value = "select * from person p " +
            "join person_addresses b " +
            "   on p.id = b.person_id " +
            "join address c " +
            "   on c.id = b.addresses_id " +
            "where c.zip = :zip", nativeQuery = true)
    Iterable<Person> getPeopleWithZip(@Param("zip") String zip);

**/




		--------!!! PROBLEMS !!!---------

 https://grails-plugins.github.io/grails-spring-security-acl/v3/index.html
 https://docs.spring.io/spring-security/site/docs/4.0.x/reference/htmlsingle/#domain-acls
 LDAP
 https://docs.spring.io/spring-data/data-jpa/docs/1.1.x/reference/html/#jpa.query-methods.query-creation	
 https://spring.io/blog/2011/04/26/advanced-spring-data-jpa-specifications-and-querydsl/
 
 
 http://krams915.blogspot.com/2011/01/spring-security-3-full-acl-tutorial.html
 
 Using pagination in the JPQL query definition is straightforward:


@Query(value = "SELECT u FROM User u ORDER BY id")
Page<User> findAllUsersWithPagination(Pageable pageable);
We can pass a PageRequest parameter to get a page of data. 
Pagination is also supported for native queries but requires a little bit of additional work.
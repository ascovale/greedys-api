package com.application.controller;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.application.persistence.dao.ImageRepository;
import com.application.persistence.dao.user.UserDAO;
import com.application.persistence.model.Image;
import com.application.persistence.model.user.User;
import com.application.service.RestaurantService;

@Controller
public class ImageController {
	@Autowired
	private UserDAO userRepository;
	@Autowired
	private ImageRepository imageRepository;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private Path rootLocation;    

	public ImageController(UserDAO userRepository, Path rootLocation, ImageRepository imageRepository) {
		this.userRepository = userRepository;
		this.rootLocation = rootLocation;
		this.imageRepository = imageRepository;
	}
	

	@GetMapping("/listImages")
	public String listUploadedFiles(Model model, Principal principal) throws Exception {
		if (principal == null) {
			return "redirect:/find";
		}

		User user = userRepository.findByEmail(principal.getName());
		/*Questo codice va tutta in una service e va fatto transactional */

		/*List<String> stringss = user.getRestaurants().getRestaurantImages().stream()
				.map(image -> this.rootLocation.resolve(image.getName()))
				.map(path -> MvcUriComponentsBuilder
						.fromMethodName(ImageController.class, "serveFile", path.getFileName().toString()).build()
						.toString())				
				.collect(Collectors.toList());
		model.addAttribute("files", stringss);*/
		return "upload";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws MalformedURLException {

		Path file = this.rootLocation.resolve(filename);
		Resource resource = new UrlResource(file.toUri());

		return ResponseEntity
				.ok()
				.body(resource);	
	}

	@PostMapping("/imageUpload")
	public String handleFileUpload(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes,
			Principal principal) throws Exception {

		if (file.getSize() == 0) {
			return "redirect:/";
		}
		//questo deve andare tutto sul service
		String uuid = UUID.randomUUID().toString();

		String imagePath = this.rootLocation.resolve(uuid + ".jpg").toString();
		Image image = new Image(imagePath);
		Files.copy(file.getInputStream(), this.rootLocation.resolve(imagePath));
		//deve essere aggiunto al ristorante non all'utente
		//restaurantService.addImage(getCurrentUser().getRestaurants(),image);
		return "redirect:/findphoto.html";
	}

	@GetMapping("/find")
	public String findPhotos(Model model) {
		return "findphoto";
	}
	
	@GetMapping("/search")
	public String findPhotos(@RequestParam String name, Model model)  {

		User user;
		try {
			user = userRepository.findByEmail(getCurrentUser().getEmail());
		} catch (Exception e) {
			return "redirect:/find";
		}
		/* 
		List<String> userImages = user.getRestaurants().getRestaurantImages(
				).stream()
				.map(image -> this.rootLocation.resolve(image.getName()))
				.map(path -> MvcUriComponentsBuilder
						.fromMethodName(ImageController.class, "serveFile", path.getFileName().toString()).build()
						.toString())				
				.collect(Collectors.toList());
		
		model.addAttribute("files", userImages);
		model.addAttribute("user", user.getName());
		*/
		return "findphoto";

	}

	@RequestMapping("/delete")
	public String findPhotos(Principal principal, @RequestParam String text, String string) throws Exception {

		User user = userRepository.findByEmail(principal.getName());

		text = text.substring(text.lastIndexOf("/"));
		text = this.rootLocation + text;

		Image image = imageRepository.findByName(text);

		//user.getRestaurants().getRestaurantImages().remove(image);

		userRepository.save(user);

		return "redirect:/";

	}
	
	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof User) {
			return ((User) principal);
		} else {
			System.out.println("Questo non dovrebbe succedere");
			return null;
		}
	}
}

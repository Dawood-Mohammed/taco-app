package com.example.tacoapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class TacoAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(TacoAppApplication.class, args);
	}

	@Bean
	CommandLineRunner clr(final IngredientRepo ingredientRepo){
		return new CommandLineRunner() {
			public void run(String... strings) throws Exception {
				List<Ingredient> ingredients = Arrays.asList(
						new Ingredient("FLTO","Floor Turtila", Ingredient.Type.WRAP),
						new Ingredient("COBR","Corn Bread", Ingredient.Type.WRAP),
						new Ingredient("DITO","Diced Tommato", Ingredient.Type.VEGGIE),
						new Ingredient("ONSL","Onion Slices", Ingredient.Type.VEGGIE),
						new Ingredient("MAYO","Mayonees Souce", Ingredient.Type.SOUCE),
						new Ingredient("SOSO","Soya Souce", Ingredient.Type.SOUCE)
				);
				ingredientRepo.saveAll(ingredients);
			}
		};
	}

}

//controllers
@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
@SessionAttributes("order")
class OrderController{
	private final OrderRepo orderRepo;

	@GetMapping("/current")
	public String orderForm(Model model){
		return "order";
	}

	@PostMapping
	public String submitOrder(@Valid Order order, Errors errors , Model model, SessionStatus status){
		if(errors.hasErrors())
			return "order";
		System.out.println("order : "+orderRepo.save(order).toString());
		model.addAttribute("name", order.getName());
		status.setComplete();
		return "depart";
	}
}
@Controller
@RequestMapping("/taco")
@RequiredArgsConstructor
@SessionAttributes("order")
class TacoController {
	private final TacoRepo tacoRepo;
	private final IngredientRepo ingredientRepo;

	@ModelAttribute("order")
	public Order order(){
		return new Order();
	}
	@GetMapping
	public String ingredients(Model model){
		List<Ingredient> ingredients = ingredientRepo.findAll();
		Ingredient.Type[] types = Ingredient.Type.values();
		for(Ingredient.Type type : types){
			model.addAttribute(type.toString().toLowerCase(), filterByType(type, ingredients));
		}
		model.addAttribute("taco", new Taco());
		return "design";
	}

	@PostMapping
	public String processDesign(@Valid Taco taco, Errors errors , Model model, @ModelAttribute Order order){
		if(errors.hasErrors()) {
			if(taco.getIngredients().size()==0)
				taco.setIngredients(new ArrayList<>());
			if(taco.getName()==null)
				taco.setName("anonymous");
		}
		System.out.println("taco : "+tacoRepo.save(taco).toString());
		order.getTacos().add(taco);
		return "redirect:/order/current";
	}

	private List<Ingredient> filterByType(Ingredient.Type type, List<Ingredient> ingredientList){
		return ingredientList.stream()
				.filter(ingredient -> ingredient.getType()==type).collect(Collectors.toList());
	}
}

//dao layer
//Ingredient dao
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
class Ingredient implements Serializable{
	@Id
	private String id;
	private String name;
	private Type type;

	public static enum Type{WRAP, SOUCE, VEGGIE}
}
@Repository
interface IngredientRepo extends JpaRepository<Ingredient, String>{}


//Taco dao
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
class Taco implements Serializable{
	@Id
	@GeneratedValue
	private Long id;
	@NotBlank(message = "Name is required")
	private String name;
	@OneToMany(orphanRemoval = true)
	@Size(min = 1, message = "At least One Ingredient must be selected")
	private List<Ingredient> ingredients = new ArrayList<Ingredient>();
}
@Repository
interface TacoRepo extends JpaRepository<Taco, Long>{}


// Order dao
@Entity
@Table(name="taco_order")
@AllArgsConstructor
@NoArgsConstructor
@Data
class Order implements Serializable{
	@Id
	@GeneratedValue
	private Long id;
	@NotBlank(message = "name is required")
	private String name;
	@NotBlank(message = "Address is required")
	private String address;
	@NotBlank(message = "Cridit Card Number is required")
	private String ccNumber;
	@Pattern(regexp = "^(0[1-9]|1[0-2])([\\/])([1-9][0-9])$", message = "Cridit Card Expiration must be [mm/yy] formatted")
	private String ccExpiration;
	@Digits(integer = 3, fraction = 0, message = "Cridit Card CVV is required")
	private String ccCVV;
	@ManyToMany
	@JoinTable(name = "taco_order_tacos",
			joinColumns = @JoinColumn(name = "taco_id" ,referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "order_id", referencedColumnName = "id"))
	private List<Taco> tacos = new ArrayList<>();
}
@Repository
interface OrderRepo extends JpaRepository<Order, Long>{}


//security config
@Configuration
@EnableWebSecurity
class SecurityConfig extends WebSecurityConfigurerAdapter{
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication()
				.withUser("dawood")
				.password("david")
				.roles("USER");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.antMatchers("/**").permitAll();
	}
}

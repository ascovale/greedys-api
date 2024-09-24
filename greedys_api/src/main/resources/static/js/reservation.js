const serverUrl = 'http://localhost:5050';

let currentMonth = new Date().getMonth();
let currentYear = new Date().getFullYear();
let selectedDay = null;
let slotId = null;
let reservationDay = null;
let selectedCell = null;

function createPopup() {
	// Crea overlay
	const overlay = document.createElement('div');
	overlay.classList.add('overlay')
	overlay.id = 'overlay';
	overlay.onclick = closePopup;
	document.body.appendChild(overlay);
	overlay.style.display = 'none';

	// Crea popup
	const popup = document.createElement('div');
	popup.classList.add('popup')
	popup.id = 'popup';
	document.body.appendChild(popup);
	popup.style.display = 'none';

	// Crea popup-content
	const popupContent = document.createElement('div');
	popupContent.id = 'popup-content';
	
	//crea le icone
	createIcons(popup);

	popup.appendChild(popupContent);

	createCalendar(popupContent);
}

function createIcons(location) {
	// Crea icons
	const icons = document.createElement('div');
	icons.className = 'icons';
	location.appendChild(icons);

	// Icona calendario
	const calendarIcon = document.createElement('i');
	calendarIcon.className = 'fas fa-calendar icon';
	calendarIcon.style.color = '#f78da7';
	icons.appendChild(calendarIcon);
	const arrow1 = document.createElement('span');
	arrow1.className = 'arrow';
	arrow1.textContent = '>';
	icons.appendChild(arrow1);

	// Icona orologio
	const clockIcon = document.createElement('i');
	clockIcon.className = 'fas fa-clock icon';
	clockIcon.style.color = 'darkgrey';
	icons.appendChild(clockIcon);
	const arrow2 = document.createElement('span');
	arrow2.className = 'arrow';
	arrow2.textContent = '>';
	icons.appendChild(arrow2);

	// Icona utenti
	const usersIcon = document.createElement('i');
	usersIcon.className = 'fas fa-users icon';
	usersIcon.style.color = 'darkgrey';
	icons.appendChild(usersIcon);
}

function createWidget(location) {
	const widgetDiv = document.createElement('div');
    widgetDiv.style.width = '300px';
    widgetDiv.style.height = '450px';
    widgetDiv.style.backgroundColor = '#f0f0f0';
    widgetDiv.style.border = '1px solid #ccc';
    widgetDiv.style.position = 'relative';
    widgetDiv.style.padding = '10px';
    widgetDiv.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.1)';
	widgetDiv.style.display = 'flex';
	widgetDiv.style.flexDirection = 'column';

	const widgetContent = document.createElement('div');

	const parent = document.querySelector(location);
	parent.appendChild(widgetDiv);

	createIcons(widgetDiv);
	
	widgetDiv.appendChild(widgetContent);
	
	createCalendar(widgetContent);

}

function openPopup() {
	createPopup();
	document.getElementById("popup").style.display = "block";
	document.getElementById("overlay").style.display = "block";
}

function closePopup() {
	document.getElementById("popup").style.display = "none";
	document.getElementById("overlay").style.display = "none";
}

function createCalendar(location) {

	location.innerHTML = '';

	// Crea calendar-header
	const calendarHeader = document.createElement('div');
	calendarHeader.className = 'calendar-header';
	location.appendChild(calendarHeader);

	// Crea calendar-container
	const calendarContainer = document.createElement('div');
	calendarContainer.className = 'calendar-container';
	calendarHeader.appendChild(calendarContainer);

	// Crea month-wrapper
	const monthWrapper = document.createElement('div');
	monthWrapper.className = 'month-wrapper';
	calendarContainer.appendChild(monthWrapper);

	// Freccia mese precedente
	const prevMonth = document.createElement('span');
	prevMonth.className = 'prev-month';
	prevMonth.id = 'prev-month';
	prevMonth.textContent = '<';
	prevMonth.addEventListener('click', () => {
		currentMonth -= 1;
		if (currentMonth < 0) {
			currentMonth = 11;
			currentYear -= 1;
		}
		updateCalendar();
	});
	monthWrapper.appendChild(prevMonth);

	// Nome mese corrente
	const currentMonthYear = document.createElement('h2');
	currentMonthYear.className = 'month month-name';
	currentMonthYear.id = 'current-month-year';
	monthWrapper.appendChild(currentMonthYear);

	// Freccia mese successivo
	const nextMonth = document.createElement('span');
	nextMonth.className = 'next-month';
	nextMonth.textContent = '>';
	nextMonth.addEventListener('click', () => {
		currentMonth += 1;
		if (currentMonth > 11) {
			currentMonth = 0;
			currentYear += 1;
		}
		updateCalendar();
	});
	monthWrapper.appendChild(nextMonth);

	// Crea calendario
	const calendar = document.createElement('div');
	calendar.className = 'calendar';
	calendarContainer.appendChild(calendar);

	// Crea bottone footer
	const footerButton = document.createElement('button');
	footerButton.className = 'footer-button';
	footerButton.id = 'select-day-button';
	footerButton.disabled = true;
	footerButton.textContent = 'SELECT DAY';
	footerButton.addEventListener('click', () => {
		if (selectedDay) {

			// Effettua la richiesta GET a /restaurant/{id}/slots

			const url = new URL(serverUrl + '/restaurant/' + id + '/day-slots');
			url.searchParams.append('date', reservationDay[0] + '-' + reservationDay[1] + '-' + reservationDay[2]);
			fetch(url.toString())
				.then(response => {
					const contentType = response.headers.get("content-type");
					if (contentType && contentType.indexOf("application/json") !== -1) {
						return response.json();
					} else {
						throw new TypeError("Oops, non abbiamo ricevuto un JSON!");
					}
				})
				.then(data => {

					icon1 = document.querySelector('.fas.fa-calendar.icon');
					icon1.style.color = 'darkgrey';
					icon2 = document.querySelector('.fas.fa-clock.icon');
					icon2.style.color = '#f78da7';

					const slotMap = new Map();
					data.forEach(d => {
						if (!slotMap.has(d.service.name)) {
							slotMap.set(d.service.name, [d]);
						}
						else {
							slotMap.get(d.service.name).push(d);
						}
					});
					createSlotTables(slotMap, location);
				})
				.catch(error => console.error('Error:', error));
		}
	});
	calendarContainer.appendChild(footerButton);

	updateCalendar(calendar);

}

function selectDay(dayElement, date) {
	if (selectedDay) {
		selectedDay.classList.remove('selected');
	}
	selectedDay = dayElement;
	selectedDay.classList.add('selected');
	document.getElementById('select-day-button').disabled = false;
	reservationDay = date;
	document.getElementById('select-day-button').style.backgroundColor = '#f78da7';
}


function updateCalendar(calendar) {
	const daysOfWeek = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
	const today = new Date();
	const year = currentYear;
	const month = currentMonth;
	const daysInMonth = new Date(year, month + 1, 0).getDate();
	const firstDayOfMonth = new Date(year, month, 1).getDay();

	// Set current month and year
	document.getElementById('current-month-year').textContent = new Date(year, month).toLocaleString('default', { month: 'long', year: 'numeric' });

	const url = new URL(serverUrl + '/restaurant/' + id + '/open-days');
	url.searchParams.append('start', new Date(currentYear, currentMonth, 1).toISOString());
	url.searchParams.append('end', new Date(currentYear, currentMonth + 1, 0).toISOString());
	fetch(url.toString())
		.then(response => {
			if (!response.ok) {
				throw new Error('Network response was not ok');
			}
			return response.json();
		})
		.then(openDays => {
			openDays.forEach(date => {
				date = date.split('-');
				const dayElement = document.getElementById('day-' + date[2]);
				if (dayElement) {
					dayElement.classList.remove('disabled');
					dayElement.addEventListener('click', () => selectDay(dayElement, date));
				}
			});
		})
		.catch(error => { console.error('There has been a problem with your fetch operation:', error) })
	//reset calendar
	calendar.innerHTML = '';

	// Create days of the week
	daysOfWeek.forEach(day => {
		const dayElement = document.createElement('div');
		dayElement.classList.add('day', 'disabled', 'day-of-week'); // Aggiungi la classe 'day-of-week'
		dayElement.textContent = day;
		calendar.appendChild(dayElement);
	});

	// Fill in days
	for (let i = 0; i < firstDayOfMonth; i++) {
		const emptyDay = document.createElement('div');
		emptyDay.classList.add('day', 'disabled');
		calendar.appendChild(emptyDay);
	}
	for (let i = 1; i <= daysInMonth; i++) {
		const dayElement = document.createElement('div');
		dayElement.classList.add('day', 'disabled');
		dayElement.textContent = i;
		dayElement.id = 'day-' + i;

		calendar.appendChild(dayElement);
	}

}

function selectCell(cell) {
	if (selectedCell) {
		selectedCell.style.backgroundColor = 'white';
	}
	selectedCell = cell;
	slotId = cell.getAttribute('data-slot-id');

	selectedCell.style.backgroundColor = '#92e7c8';
	document.getElementById('select-cell-button').disabled = false;
	document.getElementById('select-cell-button').style.backgroundColor = '#f78da7';
}


function createSlotTables(slotMap, location) {
	if (slotMap == null) {
		console.log("Services are null");
	}
	if (slotMap) {

		location.innerHTML = '';

		const services = slotMap.keys();

		function showServiceSlots(service) {
			
			const slots = slotMap.get(service);
			slotContainer.innerHTML = '';
			slots.forEach(slot => {
				const slotElement = document.createElement('div');
				slotElement.classList.add('cell-slot');
				slotElement.textContent = slot.start + ' - ' + slot.end;
				slotElement.setAttribute('data-slot-id', slot.id);
				slotElement.setAttribute('data-slot-start', slot.start);
				slotElement.addEventListener('click', () => selectCell(slotElement));
				slotContainer.appendChild(slotElement);
			});
		}

		// Contenitore per i bottoni
		const buttonContainer = document.createElement('div');
		buttonContainer.classList.add('service-container');
		location.appendChild(buttonContainer);

		// contenitore per gli slot orari
		const slotContainer = document.createElement('div');
		slotContainer.classList.add('table-slot');
		location.appendChild(slotContainer);

		for (const service of services) {
			const serviceButton = document.createElement('button');
			serviceButton.classList.add('service-text');
			serviceButton.textContent = service;
			serviceButton.addEventListener('click', () => {
				document.querySelectorAll('.service-text').forEach(btn => btn.classList.remove('service-selected'));
				serviceButton.classList.add('service-selected');
				showServiceSlots(service);
			});
			buttonContainer.appendChild(serviceButton);
		};

		let button = document.createElement('button');
		button.classList.add('footer-button');
		button.id = 'select-cell-button';
		button.textContent = 'SELECT THE TIME';
		button.addEventListener('click', () => {
			if (selectedCell) {
				createReservationForm(location);
			}
		});
		location.appendChild(button);
	}
	else {
		console.log("Nessun dato disponibile per la creazione della tabella.");
	}
}

function createReservationForm(location) {

	location.innerHTML = '';

	icon2 = document.querySelector('.fas.fa-clock.icon');
	icon2.style.color = 'darkgrey';
	icon3 = document.querySelector('.fas.fa-users.icon');
	icon3.style.color = '#f78da7';	

	let formContainer = document.createElement('div');
	formContainer.classList.add('form-container');

	const displayContainer = document.createElement('div');

	const dateLabel = document.createElement('label');
	dateLabel.textContent = 'Booking for: ';
	const dateDisplay = document.createElement('span');
	dateDisplay.textContent = reservationDay[0] + '-' + reservationDay[1] + '-' + reservationDay[2];

	displayContainer.appendChild(dateLabel);
	displayContainer.appendChild(dateDisplay);

	const timeLabel = document.createElement('label');
	timeLabel.textContent = ' at ';
	const timeDisplay = document.createElement('span');
	const time = selectedCell.getAttribute('data-slot-start');
	timeDisplay.textContent = time[0] + time[1] + ':' + time[3] + time[4];

	displayContainer.appendChild(timeLabel);
	displayContainer.appendChild(timeDisplay);

	const form = document.createElement('form');
	form.classList.add('reservation-form');

	const nameLabel = document.createElement('label');
	nameLabel.textContent = 'Name:';
	const nameInput = document.createElement('input');
	nameInput.type = 'text';
	nameInput.name = 'name';
	nameInput.required = true;

	const surnameLabel = document.createElement('label');
	surnameLabel.textContent = 'Surname:';
	const surnameInput = document.createElement('input');
	surnameInput.type = 'text';
	surnameInput.name = 'surname';
	surnameInput.required = true;

	const emailLabel = document.createElement('label');
	emailLabel.textContent = 'Email:';
	const emailInput = document.createElement('input');
	emailInput.type = 'email';
	emailInput.name = 'email';
	emailInput.required = true;

	const phoneLabel = document.createElement('label');
	phoneLabel.textContent = 'Phone number:';
	const phoneInput = document.createElement('input');
	phoneInput.type = 'tel';
	phoneInput.name = 'phone';
	phoneInput.required = true;

	const guestsLabel = document.createElement('label');
	guestsLabel.textContent = 'People:';
	const guestsInput = document.createElement('input');
	guestsInput.type = 'number';
	guestsInput.name = 'pax';
	guestsInput.required = true;

	const childrenLabel = document.createElement('label');
	childrenLabel.textContent = 'Kids:';
	const childrenInput = document.createElement('input');
	childrenInput.type = 'number';
	childrenInput.name = 'kids';
	childrenInput.required = true;

	const notesLabel = document.createElement('label');
	notesLabel.textContent = 'Notes:';
	const notesInput = document.createElement('input');
	notesInput.type = 'text';
	notesInput.name = 'notes';
	notesInput.required = true;

	const submitButton = document.createElement('button');
	submitButton.type = 'submit';
	submitButton.classList.add('footer-button');
	submitButton.textContent = 'Send';
	submitButton.addEventListener('click', (event) => {
		event.preventDefault();
		const form = document.querySelector('.reservation-form');
		const formData = new FormData(form);

		// Ottenere singoli valori dalla FormData
		const name = formData.get('name');
		const surname = formData.get('surname');
		const email = formData.get('email');
		const pax = formData.get('pax');
		const kids = formData.get('kids');
		const notes = formData.get('notes');

		console.log('Name:', name);
		console.log('Surname:', surname);

		
	
		const clientInfo = {
			name: name,
			surname: surname,
			email: email
		};

		const NewReservationDTO = {
			idSlot: slotId,
			reservationDay: reservationDay[0] + '-' + reservationDay[1] + '-' + reservationDay[2],
			restaurant_id: id,
			user_id: null,
			clientUser: clientInfo,
			pax: pax,
			kids: kids,
			notes: notes
		};
	

		fetch(serverUrl + '/reservation/', {
			method: 'POST',
			headers: {
                'Content-Type': 'application/json'
            },
			body: JSON.stringify(NewReservationDTO),
		})
			.then(response => {
				if (response.ok) {
					console.log('Reservation created successfully.');
					// TODO: Handle success case
				} else {
					console.error('Error creating reservation.');
					// TODO: Handle error case
				}
			})
			.catch(error => {
				console.error('An error occurred:', error);
				// TODO: Handle error case
			});
	});

	form.addEventListener('input', () => {
		const inputs = form.querySelectorAll('input');
		const isFormValid = Array.from(inputs).every(input => input.checkValidity());
		submitButton.style.backgroundColor = isFormValid ? '#92e7c8' : '#f78da7';
	});
	const br1 = document.createElement('br');
	const br2 = document.createElement('br');
	const br3 = document.createElement('br');
	const br4 = document.createElement('br');
	const br5 = document.createElement('br');
	const br6 = document.createElement('br');
	const br7 = document.createElement('br');
	const br8 = document.createElement('br');
	const br9 = document.createElement('br');
	const br10 = document.createElement('br');
	const br11 = document.createElement('br');
	const br12 = document.createElement('br');
	const br13 = document.createElement('br');
	form.appendChild(nameLabel);
	form.appendChild(br1);
	form.appendChild(nameInput);
	form.appendChild(br2);
	form.appendChild(surnameLabel);
	form.appendChild(br3);
	form.appendChild(surnameInput);
	form.appendChild(br4);
	form.appendChild(emailLabel);
	form.appendChild(br12);
	form.appendChild(emailInput);
	form.appendChild(br13);
	form.appendChild(phoneLabel);
	form.appendChild(br5);
	form.appendChild(phoneInput);
	form.appendChild(br6);
	form.appendChild(guestsLabel);
	form.appendChild(br7);
	form.appendChild(guestsInput);
	form.appendChild(br8);
	form.appendChild(childrenLabel);
	form.appendChild(br9);
	form.appendChild(childrenInput);
	form.appendChild(br10);
	form.appendChild(notesLabel);
	form.appendChild(br11);
	form.appendChild(notesInput);
	
	formContainer.appendChild(form);
	location.appendChild(formContainer);
	location.appendChild(submitButton);
}
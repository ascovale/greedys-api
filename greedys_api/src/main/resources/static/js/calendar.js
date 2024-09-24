
var notAvailableDays = /*[[${notAvailableDays}]]*/ null;
let currentMonth = new Date().getMonth();
let currentYear = new Date().getFullYear();
let selectedDay = null;

function createCalendar() {
	const daysOfWeek = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
	const today = new Date();
	const year = currentYear;
	const month = currentMonth;
	const daysInMonth = new Date(year, month + 1, 0).getDate();
	const firstDayOfMonth = new Date(year, month, 1).getDay();

	const calendar = document.querySelector('.calendar');
	calendar.innerHTML = '';

	// Set current month and year
	document.getElementById('current-month-year').textContent = new Date(year, month).toLocaleString('default', { month: 'long', year: 'numeric' });

	// Create days of the week
	// Create days of the week
	daysOfWeek.forEach(day => {
		const dayElement = document.createElement('div');
		dayElement.classList.add('day', 'disabled', 'day-of-week'); // Aggiungi la classe 'day-of-week'
		dayElement.textContent = day;
		calendar.appendChild(dayElement);
	});
	if (notAvailableDays != null) {

		// Fill in days
		for (let i = 0; i < firstDayOfMonth; i++) {
			const emptyDay = document.createElement('div');
			emptyDay.classList.add('day', 'disabled');
			calendar.appendChild(emptyDay);
		}
		for (let i = 1; i <= daysInMonth; i++) {
			const dayElement = document.createElement('div');
			dayElement.classList.add('day');
			dayElement.textContent = i;
			if (new Date(year, month, i) < today || notAvailableDays.includes(currentDate)) {
				dayElement.classList.add('disabled');
				dayElement.classList.add('disabled');
			} else {
				dayElement.addEventListener('click', () => selectDay(dayElement));
			}
			calendar.appendChild(dayElement);
		}
	}
	else {
		// Fill in days
		for (let i = 0; i < firstDayOfMonth; i++) {
			const emptyDay = document.createElement('div');
			emptyDay.classList.add('day', 'disabled');
			calendar.appendChild(emptyDay);
		}
		for (let i = 1; i <= daysInMonth; i++) {
			const dayElement = document.createElement('div');
			dayElement.classList.add('day');
			dayElement.textContent = i;
			if (new Date(year, month, i) < today) {
				dayElement.classList.add('disabled');
				dayElement.classList.add('disabled');
			} else {
				dayElement.addEventListener('click', () => selectDay(dayElement));
			}
			calendar.appendChild(dayElement);
		}
	}
}

function selectDay(dayElement) {
	if (selectedDay) {
		selectedDay.classList.remove('selected');
	}
	selectedDay = dayElement;
	selectedDay.classList.add('selected');
	document.getElementById('select-day-button').disabled = false;

	document.getElementById('select-day-button').style.backgroundColor = '#f78da7';
}

function updateCalendar() {
	createCalendar();
	selectedDay = null;
	document.getElementById('select-day-button').disabled = true;
}

document.querySelector('.prev-month').addEventListener('click', () => {
	currentMonth -= 1;
	if (currentMonth < 0) {
		currentMonth = 11;
		currentYear -= 1;
	}
	updateCalendar();
});

document.querySelector('.next-month').addEventListener('click', () => {
	currentMonth += 1;
	if (currentMonth > 11) {
		currentMonth = 0;
		currentYear += 1;
	}
	updateCalendar();
});

document.getElementById('select-day-button').addEventListener('click', () => {
	if (selectedDay) {
		const selectedDate = new Date(currentYear, currentMonth, parseInt(selectedDay.textContent));

		// Creazione di un oggetto FormData per inviare i dati tramite POST
		const formData = new FormData();
		formData.append('idRestaurant', 1);
		formData.append('selectedDate', selectedDate.toISOString());

		// Effettua la richiesta POST a /get-slots
		fetch('/get-slots', {
			method: 'POST',
			body: formData
		})
			.then(response => {
				const contentType = response.headers.get("content-type");
				if (contentType && contentType.indexOf("application/json") !== -1) {
					return response.json();
				} else {
					throw new TypeError("Oops, non abbiamo ricevuto un JSON!");
				}
			})
			.then(data => {
				document.querySelector('.calendar').remove();
				document.querySelector('.calendar-container').remove();
				document.querySelector('#select-day-button').remove();
				icon1 = document.querySelector('.fas.fa-calendar.icon');

				icon1.style.color = 'darkgrey';
				icon2 = document.querySelector('.fas.fa-clock.icon');
				icon2.style.color = '#f78da7';
				createSlotTables(data.services);
			})
			.catch(error => console.error('Error:', error));
	}
});

let selectedCell = null;

function selectCell(cell) {
	if (selectedCell) {
		selectedCell.style.backgroundColor = 'white';
	}
	selectedCell = cell;
	selectedCell.style.backgroundColor = '#92e7c8';
	document.getElementById('select-cell-button').disabled = false;
	document.getElementById('select-cell-button').style.backgroundColor = '#f78da7';
}

function createSlotTables(services) {
	if (services == null) {
		console.log("Services are null");
	}

	if (services) {
		console.log("Services are not null");
		let table = document.querySelector('#popup-content');

		for (let i = 0; i < services.length; i++) {
			const service = services[i];
			let tableContainer = document.createElement('div');
			tableContainer.classList.add('table-container');
			let selectText = document.createElement('div');
			selectText.classList.add('service-text');
			selectText.textContent = service.serviceType + ' ' + service.name;
			tableContainer.appendChild(selectText);
			let tableSlot = document.createElement('div');
			tableSlot.classList.add('table-slot');
			tableContainer.appendChild(tableSlot);
			table.appendChild(tableContainer);

			for (let j = 0; j < service.slots.length; j++) {
				const cell = document.createElement('div');
				cell.classList.add('cell-slot');
				cell.textContent = service.slots[j].start.slice(0, 5);
				cell.addEventListener('click', () => selectCell(cell));
				tableSlot.appendChild(cell);
			}
		}
		let button = document.createElement('button');
		button.classList.add('footer-button');
		button.id = 'select-cell-button';
		button.textContent = 'SELECT THE TIME';
		button.addEventListener('click', () => {
			if (selectedCell) {
				const selectedValue = selectedCell.textContent;
				const formData = new FormData();
				formData.append('selectedValue', selectedValue);

				fetch('/form-reservation', {
					method: 'POST',
					body: formData
				}).then(response => {
					if (response.ok) {
						console.log('Valore selezionato inviato con successo.');

						tableContainer = document.querySelector('.table-container');
						const tableContainers = document.querySelectorAll('.table-container');
						tableContainers.forEach(tableContainer => {
							tableContainer.remove();
						});

						icon2 = document.querySelector('.fas.fa-clock.icon');
						icon2.style.color = 'darkgrey';
						icon3 = document.querySelector('.fas.fa-users.icon');
						icon3.style.color = '#f78da7';


					} else {
						console.error('Errore nell\'invio del valore selezionato.');
					}
				}).catch(error => {
					console.error('Si è verificato un errore:', error);
				});
			}
		});
		table.appendChild(button);
	}
	else {
		console.log("Nessun dato disponibile per la creazione della tabella.");
	}
}


window.onload = createCalendar;

var notAvailableDays = /*[[${notAvailableDays}]]*/ null;
let currentMonth = new Date().getMonth();
let currentYear = new Date().getFullYear();
let selectedDay = null;
let isFirstLoad = true;


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
			if (notAvailableDays.includes(currentDate)) {
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
			dayElement.addEventListener('click', () => selectDay(dayElement));

			calendar.appendChild(dayElement);
		}
	}
	/*CORREGGERE MI DEVE SELEZIONARE OGGI E POI IL CALENDARIO DEVE PARTIRE DA OGGI MA ANCHE DAI 
	GIORNI PASSATI */
	if (isFirstLoad) {

		const tomorrow = new Date();
		tomorrow.setDate(tomorrow.getDate() + 1);
		const nextDay = tomorrow.getDate();
		// Seleziona il giorno successivo nel mini calendario
		const dayElements = document.querySelectorAll('.day');
		dayElements.forEach((dayElement) => {
			if (dayElement.textContent == nextDay) {
				dayElement.click();
			}
		});
		isFirstLoad = false;

	}
}

function selectDay(dayElement) {

	if (selectedDay) {
		selectedDay.classList.remove('selected');
	}
	selectedDay = dayElement;
	selectedDay.classList.add('selected');
	// Ottieni il giorno, il mese e l'anno selezionati
	const selectedDayText = selectedDay.textContent;
	const monthYearElement = document.getElementById('current-month-year');
	const [selectedMonth, selectedYear] = monthYearElement.textContent.split(' ');


	// Aggiorna il contenuto dell'elemento 'agenda-day'
	const agendaDayElement = document.getElementById('agenda-day-2');
	agendaDayElement.textContent = `${selectedDayText} ${selectedMonth} ${selectedYear}`;

	// Invia la richiesta POST
	/*
	fetch('/getreservation', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify({ day: selectedDayText, month: selectedMonth, year: selectedYear }),
	})
	.then(response => response.json())
	.then(data => {
		// Ottieni l'elemento della tabella
		const table = document.querySelector('.agenda-table');

		// Rimuovi le righe esistenti
		while (table.rows.length > 1) {
			table.deleteRow(1);
		}

		// Aggiungi le nuove righe
		data.forEach(reservation => {
			const row = table.insertRow();
			row.insertCell().textContent = reservation.orario;
			row.insertCell().textContent = reservation.numeroDiPersone;
			row.insertCell().textContent = reservation.numeroDiBambini;
			// Aggiungi altri campi qui
		});
	})
	.catch((error) => {
		console.error('Error:', error);
	});*/
}



function updateCalendar() {
	createCalendar();
	selectedDay = null;
	//selectedDay = new Date().getDate();
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
document.querySelectorAll('.day').forEach(day => {
	day.addEventListener('click', () => {
		const selectedDate = new Date(currentYear, currentMonth, parseInt(day.textContent));
		const form = document.createElement('form');
		console.log("Entrato!!!");
		form.method = 'POST';
		form.action = '/get-slots';
		const selectedDateField = document.createElement('input');
		selectedDateField.type = 'hidden';
		selectedDateField.name = 'selectedDate';
		selectedDateField.value = selectedDate.toISOString();
		form.appendChild(selectedDateField);
		document.body.appendChild(form);
		form.submit();
	});
});

document.querySelector('.prev-day').addEventListener('click', function () {
	let currentDayElement = document.querySelector('.selected');
	if (currentDayElement.textContent === '1') {
		// Se il giorno corrente è il primo del mese, non fare nulla
		
		
	} else {
		let prevDayElement = currentDayElement.previousElementSibling;
		if (prevDayElement) {
			selectDay(prevDayElement);
		}
	}
});

document.querySelector('.next-day').addEventListener('click', function () {
	let currentDayElement = document.querySelector('.selected');
	let nextDayElement = currentDayElement.nextElementSibling;
	if (nextDayElement) {
		selectDay(nextDayElement);
	} else {
		// Se non c'è un giorno successivo nel mese corrente, passa al primo giorno del mese successivo
		let firstDayOfNextMonth = document.querySelector('.next-month').firstElementChild;
		if (firstDayOfNextMonth) {
			selectDay(firstDayOfNextMonth);
		}
	}
});


window.onload = createCalendar;
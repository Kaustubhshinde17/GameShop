import { useEffect, useState } from 'react'

import './App.css'



const API_URL = 'http://localhost:8080/api'



function getBookingValue(item, camelCase, snakeCase) {

  return item?.[camelCase] ?? item?.[snakeCase]

}



function formatDateTime(value) {

  if (!value) return 'Not available'



  const date = new Date(value)



  if (Number.isNaN(date.getTime())) {

    return String(value).replace('T', ' ')

  }



  return date.toLocaleString('en-IN', {

    day: '2-digit',

    month: 'short',

    year: 'numeric',

    hour: '2-digit',

    minute: '2-digit',

  })

}



function formatHour(hour) {

  if (hour === 12) return '12:00 PM'

  if (hour > 12) return `${hour - 12}:00 PM`

  return `${hour}:00 AM`

}



function App() {

  const today = new Date()

  const todayString = [

    today.getFullYear(),

    String(today.getMonth() + 1).padStart(2, '0'),

    String(today.getDate()).padStart(2, '0'),

  ].join('-')



  const [stations, setStations] = useState([])

  const [loadingStations, setLoadingStations] = useState(true)

  const [stationError, setStationError] = useState('')





  const [customerName, setCustomerName] = useState('')

  const [bookingDate, setBookingDate] = useState('')

  const [startTime, setStartTime] = useState('')

  const [duration, setDuration] = useState('1')



  const [availableStations, setAvailableStations] = useState(null)

  const [checkingAvailability, setCheckingAvailability] = useState(false)

  const [availabilityError, setAvailabilityError] = useState('')



  const [submitting, setSubmitting] = useState(false)

  const [bookingMessage, setBookingMessage] = useState('')

  const [bookingError, setBookingError] = useState('')



  async function loadStations() {

    try {

      const response = await fetch(`${API_URL}/stations`)

      if (!response.ok) throw new Error('Could not load gaming stations.')



      const data = await response.json()

      setStations(data)

      setStationError('')

    } catch (error) {

      console.error('Station loading error:', error)

      setStationError('Unable to load consoles. Make sure the backend is running.')

    } finally {

      setLoadingStations(false)

    }

  }





  useEffect(() => {

    loadStations()

  }, [])



  // Check console availability whenever the customer changes the requested slot.

  useEffect(() => {

    if (!bookingDate || !startTime || !duration) {

      setAvailableStations(null)

      setAvailabilityError('')

      setCheckingAvailability(false)

      return

    }



    const controller = new AbortController()



    async function checkAvailability() {

      setCheckingAvailability(true)

      setAvailabilityError('')

      setAvailableStations(null)



      try {

        const params = new URLSearchParams({

          startTime: `${bookingDate}T${startTime}:00`,

          durationHours: String(Number(duration)),

        })



        const response = await fetch(

          `${API_URL}/stations/availability?${params.toString()}`,

          { signal: controller.signal },

        )



        const data = await response.json().catch(() => [])



        if (!response.ok) {

          throw new Error(

            data?.error || data?.message || 'Could not check console availability.',

          )

        }



        if (!Array.isArray(data)) {

          throw new Error('The server returned an unexpected availability response.')

        }



        setAvailableStations(data)

      } catch (error) {

        if (error.name !== 'AbortError') {

          console.error('Availability check error:', error)

          setAvailabilityError(error.message || 'Could not check availability.')

          setAvailableStations([])

        }

      } finally {

        if (!controller.signal.aborted) setCheckingAvailability(false)

      }

    }



    checkAvailability()

    return () => controller.abort()

  }, [bookingDate, startTime, duration])



  async function handleBookingSubmit(event) {

    event.preventDefault()

    setSubmitting(true)

    setBookingMessage('')

    setBookingError('')



    try {

      if (!bookingDate || !startTime || !duration) {

        throw new Error('Please select a date, start time and duration.')

      }



      if (checkingAvailability) {

        throw new Error('Please wait while we check console availability.')

      }



      if (availabilityError) {

        throw new Error('Fix the availability issue before submitting your booking.')

      }



      if (!availableStations || availableStations.length === 0) {

        throw new Error('No consoles are available for this slot. Please choose another time.')

      }



      // Fetch a CSRF token before sending the state-changing booking request.
      // The customer frontend must not read the admin-only bookings list to create IDs.
      const csrfResponse = await fetch(`${API_URL}/auth/csrf`, {
        credentials: 'include',
      })
      const csrfData = await csrfResponse.json().catch(() => ({}))
      if (!csrfResponse.ok || !csrfData.token || !csrfData.headerName) {
        throw new Error('Could not initialise secure booking submission. Please refresh and try again.')
      }

      const requestBody = {
        customerName: customerName.trim(),
        startTime: `${bookingDate}T${startTime}:00`,
        durationHours: Number(duration),
      }

      const response = await fetch(`${API_URL}/bookings`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          [csrfData.headerName]: csrfData.token,
        },
        body: JSON.stringify(requestBody),
      })

      const responseData = await response.json().catch(() => ({}))

      if (!response.ok) {

        throw new Error(

          responseData.error ||

            responseData.message ||

            'Booking failed. Please try again.',

        )

      }



      const createdBookingId =

        responseData.bookingId ?? responseData.booking_id ?? 'created'



      setBookingMessage(

        `Booking #${createdBookingId} created successfully! Your booking is pending confirmation.`,

      )

      setCustomerName('')

      setBookingDate('')

      setStartTime('')

      setDuration('1')

      setAvailableStations(null)

    } catch (error) {

      console.error('Booking submission error:', error)

      setBookingError(error.message || 'Could not submit your booking. Please try again.')

    } finally {

      setSubmitting(false)

    }

  }



  function scrollToBooking() {

    document.getElementById('booking-form')?.scrollIntoView({ behavior: 'smooth' })

  }





  const slotIsSelected = Boolean(bookingDate && startTime && duration)

  const availableStationIds = new Set(

    (availableStations ?? []).map((station) =>

      Number(getBookingValue(station, 'stationId', 'station_id')),

    ),

  )



  return (

    <div className="app">

      <nav className="navbar">

        <a href="#home" className="logo">

          GAME<span>SHOP</span>

        </a>



        <div className="nav-links">

          <a href="#home">Home</a>

          <a href="#stations">Our Consoles</a>

          <a href="#booking-form">Book a Slot</a>

          <a href="#recent-bookings">Bookings</a>

        </div>



        <button className="nav-button" onClick={scrollToBooking}>

          Book Now ↗

        </button>

      </nav>



      <main>

        <section className="hero" id="home">

          <div className="hero-content">

            <p className="eyebrow">YOUR NEXT LEVEL STARTS HERE</p>

            <h1>

              PLAY HARD.

              <br />

              <span>LIVE THE GAME.</span>

            </h1>

            <p className="hero-description">

              Your space. Your squad. Your game. Experience the thrill of PS5

              gaming with your friends.

            </p>



            <div className="hero-actions">

              <button className="primary-button" onClick={scrollToBooking}>

                Book Your Session ↗

              </button>

              <a href="#stations" className="secondary-button">

                Explore Consoles

              </a>

            </div>



            <div className="hero-stats">

              <div>

                <strong>

                  {loadingStations ? '...' : String(stations.length).padStart(2, '0')}

                </strong>

                <span>GAMING CONSOLES</span>

              </div>

              <div>

                <strong>₹150</strong>

                <span>PER HOUR</span>

              </div>

              <div>

                <strong>100%</strong>

                <span>GAMING VIBES</span>

              </div>

            </div>

          </div>



          <div className="hero-visual">

            <div className="visual-glow"></div>

            <div className="visual-tag">PLAYER 01 — READY?</div>

            <div className="controller">🎮</div>

            <div className="visual-bottom">

              <span>PRESS START</span>

              <span>▶</span>

            </div>

          </div>

        </section>



        <section className="stations" id="stations">

          <div className="section-heading">

            <div>

              <p className="eyebrow">CHOOSE YOUR BATTLE STATION</p>

              <h2>

                READY TO <span>PLAY?</span>

              </h2>

            </div>

            <p>Two consoles. Endless possibilities.</p>

          </div>



          {loadingStations && <p className="api-message">Connecting to GameShop...</p>}

          {stationError && <p className="api-message error-message">{stationError}</p>}

          {!loadingStations && !stationError && stations.length === 0 && (

            <p className="api-message">No gaming stations were found.</p>

          )}



          {!loadingStations && !stationError && stations.length > 0 && (

            <div className="station-grid">

              {stations.map((station) => {

                const stationId = getBookingValue(station, 'stationId', 'station_id')

                const stationName = getBookingValue(station, 'stationName', 'station_name')

                const hourlyRate = getBookingValue(station, 'hourlyRate', 'hourly_rate')

                const operational = station.operational === true

                const isAvailableForSlot = slotIsSelected

                  ? availableStationIds.has(Number(stationId))

                  : operational

                const isUnavailable = !operational || (slotIsSelected && !isAvailableForSlot)



                return (

                  <article className="station-card" key={stationId}>

                    <div className="station-number">

                      STATION {String(stationId).padStart(2, '0')}

                    </div>

                    <div className="station-icon">🎮</div>

                    <h3>{stationName}</h3>

                    <p>Get your squad together and jump into your favourite games.</p>



                    <div className="station-status">

                      <span

                        className={`status-dot ${

                          isUnavailable ? 'status-offline' : 'status-online'

                        }`}

                      ></span>

                      {!operational

                        ? 'Out of service'

                        : checkingAvailability && slotIsSelected

                          ? 'Checking availability...'

                          : slotIsSelected

                            ? isAvailableForSlot

                              ? 'Available for selected slot'

                              : 'Unavailable for selected slot'

                            : 'Operational'}

                    </div>



                    <div className="station-footer">

                      <strong>

                        ₹{Number(hourlyRate).toFixed(0)}

                        <span> / hour</span>

                      </strong>

                      <button

                        className="text-button"

                        disabled={!operational}

                        onClick={scrollToBooking}

                      >

                        Book this console ↗

                      </button>

                    </div>

                  </article>

                )

              })}

            </div>

          )}

          {slotIsSelected && !checkingAvailability && !availabilityError && (

            <p className="api-message">

              {availableStations?.length

                ? `${availableStations.length} console(s) available for your selected slot.`

                : 'No consoles are available for your selected slot. Try another time.'}

            </p>

          )}

        </section>



        <section className="booking" id="booking">

          <p className="eyebrow">YOUR GAME TIME AWAITS</p>

          <h2>

            LET THE GAMES <span>BEGIN.</span>

          </h2>

          <p>

            Ready to challenge your friends? Choose your slot and get ready to

            play.

          </p>

          <button className="primary-button" onClick={scrollToBooking}>

            Start Booking ↗

          </button>

        </section>



        <section className="booking-form-section" id="booking-form">

          <p className="eyebrow">RESERVE YOUR SESSION</p>

          <h2>

            BOOK YOUR <span>GAME TIME.</span>

          </h2>



          <form className="booking-form" onSubmit={handleBookingSubmit}>

            <label>

              Your name

              <input

                type="text"

                name="customerName"

                placeholder="Enter your full name"

                value={customerName}

                onChange={(event) => setCustomerName(event.target.value)}

                minLength={2}

                maxLength={100}

                required

              />

            </label>



            <label>

              Booking date

              <input

                type="date"

                name="bookingDate"

                min={todayString}

                value={bookingDate}

                onChange={(event) => setBookingDate(event.target.value)}

                required

              />

            </label>



            <label>

              Start time

              <select

                name="startTime"

                value={startTime}

                onChange={(event) => setStartTime(event.target.value)}

                required

              >

                <option value="" disabled>

                  Select a time

                </option>

               {Array.from({ length: 13 }, (_, index) => index + 10).map((hour) => (

                  <option

                    key={hour}

                    value={`${String(hour).padStart(2, '0')}:00`}

                  >

                    {formatHour(hour)}

                  </option>

                ))}

              </select>

            </label>



            <label>

              Duration

              <select

                name="duration"

                value={duration}

                onChange={(event) => setDuration(event.target.value)}

                required

              >

                <option value="1">1 hour — ₹150</option>

                <option value="2">2 hours — ₹300</option>

                <option value="3">3 hours — ₹450</option>

                <option value="4">4 hours — ₹600</option>

              </select>

            </label>



            <div className="booking-total">

              <span>Estimated total</span>

              <strong>₹{150 * Number(duration)}</strong>

            </div>



            {slotIsSelected && checkingAvailability && (

              <p className="form-note" role="status">

                Checking which consoles are free for your selected slot...

              </p>

            )}



            {availabilityError && (

              <p className="form-note error-message" role="alert">

                {availabilityError}

              </p>

            )}



            {slotIsSelected && !checkingAvailability && !availabilityError && (

              <p className="form-note" role="status">

                {availableStations?.length

                  ? `${availableStations.length} console(s) available for this time.`

                  : 'No consoles are available at this time. Please choose another slot.'}

              </p>

            )}



            <button

              type="submit"

              className="primary-button"

              disabled={

                submitting ||

                checkingAvailability ||

                (slotIsSelected && availableStations !== null && availableStations.length === 0)

              }

            >

              {submitting ? 'Submitting...' : 'Submit Booking ↗'}

            </button>



            {bookingMessage && (

              <p className="form-note booking-message" role="status">

                {bookingMessage}

              </p>

            )}



            {bookingError && (

              <p className="form-note error-message" role="alert">

                {bookingError}

              </p>

            )}



            <p className="form-note">

              A submitted booking remains pending until it is confirmed.

              Availability and confirmation are checked by the backend.

            </p>

          </form>

        </section>



      </main>



      <footer>

        <a href="#home" className="logo">

          GAME<span>SHOP</span>

        </a>

        <p>GAME ON. LEVEL UP. REPEAT.</p>

        <div className="footer-bottom">

          <span>© 2026 GameShop</span>

          <a href="/admin" className="admin-login-link">

            Admin Login ↗

          </a>

        </div>

      </footer>

    </div>

  )

}



export default App

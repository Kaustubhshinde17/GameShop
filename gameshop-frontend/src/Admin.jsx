

import { useCallback, useEffect, useState } from 'react'

import './Admin.css'



const API_URL = 'http://localhost:8080/api'




async function getCsrfToken() {
  const response = await fetch(`${API_URL}/auth/csrf`, { credentials: 'include' })
  if (!response.ok) throw new Error('Could not obtain a security token. Refresh and try again.')
  return response.json()
}

function getValue(item, camelCase, snakeCase) {

  return item?.[camelCase] ?? item?.[snakeCase]

}



function formatDate(value) {

  if (!value) return '—'



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



function formatStatus(status) {

  return String(status ?? 'UNKNOWN').toUpperCase()

}



function Admin() {

  const [loggedIn, setLoggedIn] = useState(false)
  const [username, setUsername] = useState('')
  const [authChecking, setAuthChecking] = useState(true)
  const [password, setPassword] = useState('')

  const [activePage, setActivePage] = useState('Overview')



  const [bookings, setBookings] = useState([])

  const [stations, setStations] = useState([])



  const [loading, setLoading] = useState(true)

  const [error, setError] = useState('')

  const [actionMessage, setActionMessage] = useState('')

  const [busyBookingId, setBusyBookingId] = useState(null)

  // Console management form state
  const [stationBusy, setStationBusy] = useState(false)
  const [editingStationId, setEditingStationId] = useState(null)
  const [stationName, setStationName] = useState('')
  const [stationRate, setStationRate] = useState('')
  const [stationOperational, setStationOperational] = useState(true)



  function resetStationForm() {
    setEditingStationId(null)
    setStationName('')
    setStationRate('')
    setStationOperational(true)
  }

  const loadDashboardData = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const [bookingsResponse, stationsResponse] = await Promise.all([
        fetch(`${API_URL}/bookings`, { credentials: 'include' }),
        fetch(`${API_URL}/stations`, { credentials: 'include' }),
      ])

      if (bookingsResponse.status === 401 || bookingsResponse.status === 403) {
        setLoggedIn(false)
        throw new Error('Your admin session has expired. Please sign in again.')
      }
      if (!bookingsResponse.ok || !stationsResponse.ok) {
        throw new Error('Could not load dashboard data from the backend.')
      }

      const [bookingData, stationData] = await Promise.all([
        bookingsResponse.json(),
        stationsResponse.json(),
      ])
      if (!Array.isArray(bookingData) || !Array.isArray(stationData)) {
        throw new Error('The server returned an unexpected response.')
      }

      const sortedBookings = [...bookingData].sort((a, b) => {
        const aId = Number(getValue(a, 'bookingId', 'booking_id') ?? 0)
        const bId = Number(getValue(b, 'bookingId', 'booking_id') ?? 0)
        return bId - aId
      })
      setBookings(sortedBookings)
      setStations(stationData)
    } catch (loadError) {
      console.error('Admin dashboard error:', loadError)
      setError(loadError.message || 'Unable to connect to GameShop. Check the backend server.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true

    async function checkExistingSession() {
      try {
        const response = await fetch(`${API_URL}/auth/me`, { credentials: 'include' })
        if (response.ok) {
          const admin = await response.json()
          if (active) {
            setUsername(admin.username || '')
            setLoggedIn(true)
          }
        } else if (active) {
          setLoggedIn(false)
          setUsername('')
        }
      } catch (sessionError) {
        console.error('Could not check admin session:', sessionError)
        if (active) setError('Could not connect to GameShop. Check that the backend is running.')
      } finally {
        if (active) setAuthChecking(false)
      }
    }

    checkExistingSession()
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (loggedIn) loadDashboardData()
  }, [loggedIn, loadDashboardData])

  async function handleLogin(event) {
    event.preventDefault()
    setError('')
    setActionMessage('')
    if (!username.trim() || !password) return

    try {
      const csrf = await getCsrfToken()
      const formData = new URLSearchParams()
      formData.set('username', username.trim())
      formData.set('password', password)

      const response = await fetch(`${API_URL}/auth/login`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          [csrf.headerName]: csrf.token,
        },
        body: formData.toString(),
      })
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.error || 'Invalid username or password.')
      }

      const sessionResponse = await fetch(`${API_URL}/auth/me`, { credentials: 'include' })
      if (!sessionResponse.ok) {
        throw new Error('Login succeeded, but the admin session could not be verified.')
      }
      const admin = await sessionResponse.json()
      setUsername(admin.username || username.trim())
      setPassword('')
      setLoggedIn(true)
    } catch (loginError) {
      setError(loginError.message || 'Unable to sign in. Check the backend server.')
    }
  }

  async function handleLogout() {
    setError('')
    try {
      const csrf = await getCsrfToken()
      const response = await fetch(`${API_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: { [csrf.headerName]: csrf.token },
      })
      if (!response.ok) throw new Error('Could not sign out. Please try again.')
      setLoggedIn(false)
      setActivePage('Overview')
      setUsername('')
      setPassword('')
      setBookings([])
      setStations([])
      setActionMessage('')
      resetStationForm()
    } catch (logoutError) {
      setError(logoutError.message || 'Unable to sign out.')
    }
  }

  async function handleBookingAction(bookingId, action) {

    setBusyBookingId(bookingId)

    setActionMessage('')

    setError('')



    try {

      const csrf = await getCsrfToken()
      const response = await fetch(
        `${API_URL}/bookings/${bookingId}/${action}`,
        {
          method: 'PUT',
          credentials: 'include',
          headers: { [csrf.headerName]: csrf.token },
        }
      )

      const responseData = await response.json().catch(() => ({}))



      if (!response.ok) {

        throw new Error(

          responseData.error ||

            responseData.message ||

            `Could not ${action} booking #${bookingId}.`

        )

      }



      setActionMessage(

        `Booking #${bookingId} ${action === 'confirm' ? 'confirmed' : 'cancelled'} successfully.`

      )



      await loadDashboardData()

    } catch (actionError) {

      setError(actionError.message || 'The booking action failed.')

    } finally {

      setBusyBookingId(null)

    }

  }



  // Create a new console or update an existing console.
  async function handleSaveStation(event) {
    event.preventDefault()
    setError('')
    setActionMessage('')

    const name = stationName.trim()
    const rate = Number(stationRate)

    if (name.length < 2 || name.length > 50) {
      setError('Console name must contain 2–50 characters.')
      return
    }
    if (!Number.isFinite(rate) || rate <= 0) {
      setError('Enter a valid hourly rate greater than zero.')
      return
    }

    setStationBusy(true)
    try {
      const csrf = await getCsrfToken()
      const isEditing = editingStationId !== null
      const response = await fetch(
        isEditing
          ? `${API_URL}/admin/stations/${editingStationId}`
          : `${API_URL}/admin/stations`,
        {
          method: isEditing ? 'PUT' : 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token,
          },
          body: JSON.stringify({
            stationName: name,
            hourlyRate: rate,
            operational: stationOperational,
          }),
        }
      )
      const data = await response.json().catch(() => ({}))
      if (!response.ok) {
        throw new Error(data.message || data.error || 'Could not save console.')
      }

      setActionMessage(
        isEditing ? `"${name}" updated successfully.` : `"${name}" added successfully.`
      )
      resetStationForm()
      await loadDashboardData()
    } catch (err) {
      setError(err.message || 'Could not save console.')
    } finally {
      setStationBusy(false)
    }
  }

  function handleEditStation(station) {
    setEditingStationId(getValue(station, 'stationId', 'station_id'))
    setStationName(getValue(station, 'stationName', 'station_name') || '')
    setStationRate(String(getValue(station, 'hourlyRate', 'hourly_rate') ?? ''))
    setStationOperational(
      getValue(station, 'operational', 'operational') === true ||
      getValue(station, 'operational', 'operational') === 1 ||
      getValue(station, 'operational', 'operational') === '1'
    )
    setError('')
    setActionMessage('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function handleToggleStation(station) {
    setError('')
    setActionMessage('')
    setStationBusy(true)
    try {
      const csrf = await getCsrfToken()
      const stationId = getValue(station, 'stationId', 'station_id')
      const name = getValue(station, 'stationName', 'station_name')
      const rate = Number(getValue(station, 'hourlyRate', 'hourly_rate'))
      const operational = getValue(station, 'operational', 'operational') === true ||
        getValue(station, 'operational', 'operational') === 1 ||
        getValue(station, 'operational', 'operational') === '1'

      const response = await fetch(`${API_URL}/admin/stations/${stationId}`, {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify({
          stationName: name,
          hourlyRate: rate,
          operational: !operational,
        }),
      })
      const data = await response.json().catch(() => ({}))
      if (!response.ok) {
        throw new Error(data.message || data.error || 'Could not update console status.')
      }
      setActionMessage(`${name} ${operational ? 'disabled' : 'enabled'} successfully.`)
      await loadDashboardData()
    } catch (err) {
      setError(err.message || 'Could not change console status.')
    } finally {
      setStationBusy(false)
    }
  }

  // Permanently delete a console only when no bookings reference it.
  async function handleDeleteStation(station) {
    const stationId = getValue(station, 'stationId', 'station_id')
    const name = getValue(station, 'stationName', 'station_name')
    const confirmed = window.confirm(
      `Permanently delete "${name}"?\\n\\nThis cannot be undone. Consoles linked to booking history cannot be deleted.`
    )
    if (!confirmed) return

    setError('')
    setActionMessage('')
    setStationBusy(true)
    try {
      const csrf = await getCsrfToken()
      const response = await fetch(`${API_URL}/admin/stations/${stationId}`, {
        method: 'DELETE',
        credentials: 'include',
        headers: { [csrf.headerName]: csrf.token },
      })
      const data = await response.json().catch(() => ({}))
      if (!response.ok) {
        throw new Error(data.message || data.error || 'Could not delete console.')
      }
      setActionMessage(`"${name}" deleted successfully.`)
      if (editingStationId === stationId) resetStationForm()
      await loadDashboardData()
    } catch (err) {
      setError(err.message || 'Could not delete console.')
    } finally {
      setStationBusy(false)
    }
  }

  const pendingBookings = bookings.filter(

    (booking) => formatStatus(booking.status) === 'PENDING'

  )



  const confirmedBookings = bookings.filter(

    (booking) => formatStatus(booking.status) === 'CONFIRMED'

  )



  const operationalStations = stations.filter(

    (station) => station.operational === true

  )



  // Estimated value uses each booking's saved combined hourly rate.
  // It is an estimate, not payment-verified revenue.
  const estimatedValue = confirmedBookings.reduce((total, booking) => {
    const duration = Number(getValue(booking, 'durationHours', 'duration_hours') ?? 0)
    const rate = Number(getValue(booking, 'hourlyRate', 'hourly_rate') ?? 0)
    return total + duration * rate
  }, 0)



  if (authChecking) {
    return (
      <div className="admin-login-page">
        <p>Checking admin session...</p>
      </div>
    )
  }

  if (!loggedIn) {

    return (

      <div className="admin-login-page">

        <div className="admin-login-glow"></div>



        <a className="admin-brand" href="/">

          GAME<span>SHOP</span>

          <small>ADMIN PORTAL</small>

        </a>



        <form className="admin-login-card" onSubmit={handleLogin}>

          <div className="admin-login-icon">🎮</div>



          <p className="admin-eyebrow">CONTROL CENTER</p>

          <h1>Welcome back.</h1>

          <p className="admin-login-description">

            Sign in to manage GameShop bookings and consoles.

          </p>



          <label>

            Username

            <input

              type="text"

              value={username}

              onChange={(event) => setUsername(event.target.value)}

              placeholder="Enter admin username"

              autoComplete="username"

              required

            />

          </label>



          <label>

            Password

            <input

              type="password"

              value={password}

              onChange={(event) => setPassword(event.target.value)}

              placeholder="Enter password"

              autoComplete="current-password"

              required

            />

          </label>



          {error && (
            <p className="admin-alert admin-alert-error" role="alert">
              {error}
            </p>
          )}

          <button className="admin-primary-button" type="submit">
            Sign in ↗
          </button>



          <a className="admin-back-link" href="/">

            ← Back to customer website

          </a>

        </form>

      </div>

    )

  }



  const navItems = [

    { label: 'Overview', icon: '◫' },

    { label: 'Bookings', icon: '▦', count: pendingBookings.length },

    { label: 'Consoles', icon: '🎮' },

  ]



  return (

    <div className="admin-layout">

      <aside className="admin-sidebar">

        <a className="admin-brand sidebar-brand" href="/">

          GAME<span>SHOP</span>

          <small>ADMIN PORTAL</small>

        </a>



        <div className="admin-nav-label">WORKSPACE</div>



        <nav className="admin-nav">

          {navItems.map((item) => (

            <button

              type="button"

              key={item.label}

              className={`admin-nav-item ${

                activePage === item.label ? 'active' : ''

              }`}



              onClick={() => setActivePage(item.label)}

            >

              <span className="admin-nav-icon">{item.icon}</span>

              <span>{item.label}</span>



              {item.label === 'Bookings' && item.count > 0 && (

                <span className="admin-nav-count">{item.count}</span>

              )}

            </button>

          ))}

        </nav>



        <div className="admin-sidebar-bottom">

          <div className="admin-profile">

            <div className="admin-avatar">A</div>

            <div>

              <strong>{username || 'Admin'}</strong>

              <span>Authenticated session</span>

            </div>

          </div>



          <button

            type="button"

            className="admin-logout"

            onClick={handleLogout}

          >

            ↪ Sign out

          </button>



          <a className="admin-back-link" href="/">

            ← Customer website

          </a>

        </div>

      </aside>



      <main className="admin-main">

        <header className="admin-topbar">

          <div>

            <p className="admin-eyebrow">GAME ON. LEVEL UP.</p>

            <h1>{activePage}</h1>

          </div>



          <div className="admin-topbar-actions">

            <span className="admin-connection">

              <span></span> Admin preview

            </span>



            <button

              type="button"

              className="admin-refresh-button"

              onClick={loadDashboardData}

              disabled={loading}

            >

              {loading ? 'Loading...' : '↻ Refresh'}

            </button>

          </div>

        </header>



        {error && (

          <div className="admin-alert admin-alert-error" role="alert">

            {error}

          </div>

        )}



        {actionMessage && (

          <div className="admin-alert" role="status">

            {actionMessage}

          </div>

        )}



        {activePage === 'Overview' && (

          <>

            <section className="admin-welcome">

              <div>

                <p className="admin-eyebrow">YOUR GAMING BUSINESS</p>

                <h2>Everything under control.</h2>

                <p>

                  Manage reservations and keep your gaming stations running.

                </p>

              </div>

              <div className="admin-welcome-controller">🎮</div>

            </section>



            <section className="admin-stat-grid">

              <article className="admin-stat-card">

                <div className="admin-stat-top">

                  <span>Total bookings</span>

                  <span className="admin-stat-icon">▦</span>

                </div>

                <strong>{loading ? '—' : bookings.length}</strong>

                <small>All booking records</small>

              </article>



              <article className="admin-stat-card">

                <div className="admin-stat-top">

                  <span>Pending requests</span>

                  <span className="admin-stat-icon">◷</span>

                </div>

                <strong>{loading ? '—' : pendingBookings.length}</strong>

                <small>Awaiting confirmation</small>

              </article>



              <article className="admin-stat-card">

                <div className="admin-stat-top">

                  <span>Operational consoles</span>

                  <span className="admin-stat-icon">🎮</span>

                </div>

                <strong>

                  {loading ? '—' : `${operationalStations.length}/${stations.length}`}

                </strong>

                <small>Current console status</small>

              </article>



              <article className="admin-stat-card">

                <div className="admin-stat-top">

                  <span>Estimated booking value</span>

                  <span className="admin-stat-icon">₹</span>

                </div>

                <strong>

                  {loading ? '—' : `₹${estimatedValue.toLocaleString('en-IN')}`}

                </strong>

                <small>Confirmed bookings × saved rates</small>

              </article>

            </section>



            <section className="admin-panel">

              <div className="admin-panel-heading">

                <div>

                  <h2>Latest bookings</h2>

                  <p>Most recently created booking IDs</p>

                </div>



                <button

                  type="button"

                  className="admin-link-button"

                  onClick={() => setActivePage('Bookings')}

                >

                  View all ↗

                </button>

              </div>



              {loading && <p className="admin-empty">Loading bookings...</p>}



              {!loading && bookings.length === 0 && (

                <p className="admin-empty">No bookings found.</p>

              )}



              {!loading && bookings.length > 0 && (

                <BookingTable

                  bookings={bookings.slice(0, 5)}

                  stations={stations}

                  busyBookingId={busyBookingId}

                  onAction={handleBookingAction}

                />

              )}

            </section>

          </>

        )}



        {activePage === 'Bookings' && (

          <section className="admin-panel">

            <div className="admin-panel-heading">

              <div>

                <h2>Booking management</h2>

                <p>Review reservations and process pending requests.</p>

              </div>

              <span className="admin-count-label">

                {bookings.length} records

              </span>

            </div>



            {loading && <p className="admin-empty">Loading bookings...</p>}



            {!loading && bookings.length === 0 && (

              <p className="admin-empty">No bookings found.</p>

            )}



            {!loading && bookings.length > 0 && (

              <BookingTable

                bookings={bookings}

                stations={stations}

                busyBookingId={busyBookingId}

                onAction={handleBookingAction}

              />

            )}

          </section>

        )}



        {activePage === 'Consoles' && (
          <section className="admin-panel">
            <div className="admin-panel-heading">
              <div>
                <h2>Console management</h2>
                <p>Add consoles, change prices, or manage availability.</p>
              </div>
              <span className="admin-count-label">{stations.length} consoles</span>
            </div>

            <form className="admin-console-form" onSubmit={handleSaveStation}>
              <h3>
                {editingStationId !== null
                  ? `Edit console #${editingStationId}`
                  : 'Add a new console'}
              </h3>

              <label>
                Console name
                <input
                  type="text"
                  value={stationName}
                  onChange={(event) => setStationName(event.target.value)}
                  placeholder="e.g. PS5-05"
                  minLength={2}
                  maxLength={50}
                  required
                />
              </label>

              <label>
                Hourly rate (₹)
                <input
                  type="number"
                  value={stationRate}
                  onChange={(event) => setStationRate(event.target.value)}
                  placeholder="e.g. 150"
                  min="0.01"
                  step="0.01"
                  required
                />
              </label>

              {editingStationId !== null && (
                <label className="admin-console-checkbox">
                  <input
                    type="checkbox"
                    checked={stationOperational}
                    onChange={(event) => setStationOperational(event.target.checked)}
                  />
                  Console is operational
                </label>
              )}

              <div className="admin-console-form-actions">
                <button
                  className="admin-primary-button"
                  type="submit"
                  disabled={stationBusy}
                >
                  {stationBusy
                    ? 'Saving...'
                    : editingStationId !== null
                      ? 'Save changes'
                      : 'Add console'}
                </button>
                {editingStationId !== null && (
                  <button
                    type="button"
                    className="admin-refresh-button"
                    onClick={resetStationForm}
                    disabled={stationBusy}
                  >
                    Stop editing
                  </button>
                )}
              </div>
            </form>

            <div className="admin-console-list-heading">
              <h3>Your consoles</h3>
              <span>{stations.length} total</span>
            </div>

            {loading && <p className="admin-empty">Loading consoles...</p>}
            {!loading && stations.length === 0 && (
              <p className="admin-empty">No consoles found. Add your first console above.</p>
            )}

            {!loading && stations.length > 0 && (
              <div className="admin-console-grid">
                {stations.map((station) => {
                  const id = getValue(station, 'stationId', 'station_id')
                  const name = getValue(station, 'stationName', 'station_name')
                  const rate = getValue(station, 'hourlyRate', 'hourly_rate')
                  const operational =
                    getValue(station, 'operational', 'operational') === true ||
                    getValue(station, 'operational', 'operational') === 1 ||
                    getValue(station, 'operational', 'operational') === '1'

                  return (
                    <article className="admin-console-card" key={id}>
                      <div className="admin-console-art">🎮</div>
                      <p className="admin-eyebrow">STATION {String(id).padStart(2, '0')}</p>
                      <h3>{name}</h3>
                      <strong>₹{Number(rate).toLocaleString('en-IN')} / hour</strong>
                      <span className={`admin-status-pill ${operational ? 'available' : 'unavailable'}`}>
                        {operational ? 'Operational' : 'Unavailable'}
                      </span>
                      <div className="admin-row-actions admin-console-actions">
                        <button
                          type="button"
                          className="admin-refresh-button"
                          onClick={() => handleEditStation(station)}
                          disabled={stationBusy}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className={operational ? 'admin-cancel-button' : 'admin-confirm-button'}
                          onClick={() => handleToggleStation(station)}
                          disabled={stationBusy}
                        >
                          {operational ? 'Disable' : 'Enable'}
                        </button>
                        <button
                          type="button"
                          className="admin-cancel-button"
                          onClick={() => handleDeleteStation(station)}
                          disabled={stationBusy}
                        >
                          Delete
                        </button>
                      </div>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        )}

        <footer className="admin-footer">

          <span>GAME<span className="admin-footer-brand">SHOP</span></span>

          <span>Admin workspace</span>

        </footer>

      </main>

    </div>

  )

}



function BookingTable({ bookings, stations = [], busyBookingId, onAction }) {
  const stationNameById = new Map(
    stations.map((station) => [
      Number(getValue(station, 'stationId', 'station_id')),
      getValue(station, 'stationName', 'station_name'),
    ])
  )

  return (
    <div className="admin-table-wrap">
      <table className="admin-table">
        <thead>
          <tr>
            <th>Booking</th>
            <th>Customer</th>
            <th>Session</th>
            <th>Duration</th>
            <th>Consoles</th>
            <th>Price breakdown</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          {bookings.map((booking) => {
            const id = getValue(booking, 'bookingId', 'booking_id')
            const name = getValue(booking, 'customerName', 'customer_name')
            const start = getValue(booking, 'startTime', 'start_time')
            const end = getValue(booking, 'endTime', 'end_time')
            const duration = Number(
              getValue(booking, 'durationHours', 'duration_hours') ?? 0
            )
            const combinedRate = Number(
              getValue(booking, 'hourlyRate', 'hourly_rate') ?? 0
            )
            const status = formatStatus(booking.status)
            const isBusy = busyBookingId === id

            const bookingStations =
              getValue(booking, 'bookingStations', 'booking_stations') || []

            const legacyStationId = getValue(
              booking,
              'assignedStationId',
              'assigned_station_id'
            )

            const selectedStations = bookingStations.length > 0
              ? bookingStations
              : legacyStationId != null
                ? [{ stationId: legacyStationId, hourlyRate: combinedRate }]
                : []

            const totalCost = duration * combinedRate

            return (
              <tr key={id}>
                <td><strong>#{id}</strong></td>
                <td>{name || '—'}</td>
                <td>
                  {formatDate(start)}
                  <small className="admin-table-subtext">
                    Until {formatDate(end)}
                  </small>
                </td>
                <td>{duration || '—'} h</td>
                <td>
                  {selectedStations.length > 0 ? (
                    <div className="admin-booking-consoles">
                      {selectedStations.map((station, index) => {
                        const stationId = getValue(station, 'stationId', 'station_id')
                        const stationName = stationNameById.get(Number(stationId))
                        return (
                          <span key={`${stationId}-${index}`}>
                            {stationName || `Console #${stationId}`}
                          </span>
                        )
                      })}
                    </div>
                  ) : 'Not assigned'}
                </td>
                <td>
                  <div className="admin-price-breakdown">
                    {selectedStations.map((station, index) => {
                      const stationId = getValue(station, 'stationId', 'station_id')
                      const stationName = stationNameById.get(Number(stationId))
                      const stationRate = Number(
                        getValue(station, 'hourlyRate', 'hourly_rate_at_booking') ?? 0
                      )
                      return (
                        <small key={`${stationId}-${index}`}>
                          {stationName || `Console #${stationId}`}: ₹{stationRate.toLocaleString('en-IN')}/h
                        </small>
                      )
                    })}
                    <strong>Combined: ₹{combinedRate.toLocaleString('en-IN')}/h</strong>
                    <strong className="admin-booking-total">
                      Total: ₹{totalCost.toLocaleString('en-IN')}
                    </strong>
                  </div>
                </td>
                <td>
                  <span className={`admin-status-pill ${status.toLowerCase()}`}>
                    {status}
                  </span>
                </td>
                <td>
                  <div className="admin-row-actions">
                    {status === 'PENDING' && (
                      <>
                        <button
                          type="button"
                          className="admin-confirm-button"
                          disabled={isBusy}
                          onClick={() => onAction(id, 'confirm')}
                        >
                          {isBusy ? 'Wait...' : 'Confirm'}
                        </button>
                        <button
                          type="button"
                          className="admin-cancel-button"
                          disabled={isBusy}
                          onClick={() => onAction(id, 'cancel')}
                        >
                          Cancel
                        </button>
                      </>
                    )}
                    {status === 'CONFIRMED' && (
                      <button
                        type="button"
                        className="admin-cancel-button"
                        disabled={isBusy}
                        onClick={() => onAction(id, 'cancel')}
                      >
                        Cancel booking
                      </button>
                    )}
                    {status !== 'PENDING' && status !== 'CONFIRMED' && (
                      <span className="admin-no-action">No action</span>
                    )}
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}


export default Admin

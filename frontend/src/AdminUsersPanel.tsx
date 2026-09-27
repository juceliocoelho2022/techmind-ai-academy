import { useEffect, useMemo, useState } from 'react'
import {
  GraduationCap,
  LoaderCircle,
  Search,
  ShieldCheck,
  UserCog,
  Users
} from 'lucide-react'
import './admin-users-panel.css'

type AdminUser = {
  id: number
  name: string
  email: string
  role: 'STUDENT' | 'ADMIN'
  createdAt: string
  enrollments: number
  completedLessons: number
  xp: number
  currentUser: boolean
}

type Props = {
  token: string
  refreshKey: number
  onMessage: (message: string) => void
}

async function adminRequest<T>(
  path: string,
  token: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${token}`)

  if (options.body) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, { ...options, headers })

  if (!response.ok) {
    const raw = await response.text()
    let detail = raw

    try {
      const payload = JSON.parse(raw)
      detail = payload.detail || payload.message || payload.title || raw
    } catch {
      // mantém resposta textual
    }

    throw new Error(detail || `HTTP ${response.status}`)
  }

  return response.json()
}

export default function AdminUsersPanel({
  token,
  refreshKey,
  onMessage
}: Props) {
  const [users, setUsers] = useState<AdminUser[]>([])
  const [query, setQuery] = useState('')
  const [roleFilter, setRoleFilter] = useState<'ALL' | 'STUDENT' | 'ADMIN'>('ALL')
  const [loading, setLoading] = useState(false)
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  async function loadUsers() {
    setLoading(true)

    try {
      const data = await adminRequest<AdminUser[]>(
        '/api/v1/admin/users',
        token
      )
      setUsers(data)
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível carregar os usuários.'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadUsers()
  }, [token, refreshKey])

  const filteredUsers = useMemo(() => {
    const normalized = query.trim().toLowerCase()

    return users.filter(user => {
      const matchesQuery =
        !normalized ||
        user.name.toLowerCase().includes(normalized) ||
        user.email.toLowerCase().includes(normalized)

      const matchesRole =
        roleFilter === 'ALL' || user.role === roleFilter

      return matchesQuery && matchesRole
    })
  }, [users, query, roleFilter])

  const stats = useMemo(() => {
    const admins = users.filter(user => user.role === 'ADMIN').length
    const students = users.filter(user => user.role === 'STUDENT').length
    const totalXp = users.reduce((total, user) => total + user.xp, 0)

    return {
      total: users.length,
      admins,
      students,
      totalXp
    }
  }, [users])

  async function changeRole(user: AdminUser, role: 'STUDENT' | 'ADMIN') {
    if (user.role === role) return

    const action = role === 'ADMIN' ? 'promover para ADMIN' : 'rebaixar para STUDENT'
    if (!window.confirm(`Deseja ${action} a conta "${user.email}"?`)) {
      return
    }

    setUpdatingId(user.id)

    try {
      const updated = await adminRequest<AdminUser>(
        `/api/v1/admin/users/${user.id}/role`,
        token,
        {
          method: 'PATCH',
          body: JSON.stringify({ role })
        }
      )

      setUsers(current =>
        current.map(item => item.id === updated.id ? updated : item)
      )

      onMessage(
        role === 'ADMIN'
          ? 'Usuário promovido para ADMIN. Ele deve entrar novamente para atualizar a sessão.'
          : 'Usuário alterado para STUDENT.'
      )
    } catch (error) {
      onMessage(
        error instanceof Error
          ? error.message
          : 'Não foi possível alterar o papel do usuário.'
      )
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <section
      className="admin-card admin-users-panel admin-scroll-target"
      id="admin-users"
    >
      <div className="admin-users-header">
        <div>
          <span className="admin-kicker">USUÁRIOS</span>
          <h2>Gestão de usuários</h2>
          <p>
            Consulte alunos e administradores, acompanhe atividade e gerencie papéis.
          </p>
        </div>

        <button className="admin-users-refresh" onClick={loadUsers} disabled={loading}>
          {loading ? <LoaderCircle className="spin" size={16} /> : <Users size={16} />}
          Atualizar
        </button>
      </div>

      <div className="admin-users-stats">
        <article>
          <Users size={20} />
          <div>
            <span>Total</span>
            <strong>{stats.total}</strong>
          </div>
        </article>
        <article>
          <GraduationCap size={20} />
          <div>
            <span>Alunos</span>
            <strong>{stats.students}</strong>
          </div>
        </article>
        <article>
          <ShieldCheck size={20} />
          <div>
            <span>Admins</span>
            <strong>{stats.admins}</strong>
          </div>
        </article>
        <article>
          <UserCog size={20} />
          <div>
            <span>XP acumulado</span>
            <strong>{stats.totalXp}</strong>
          </div>
        </article>
      </div>

      <div className="admin-users-toolbar">
        <label className="admin-user-search">
          <Search size={16} />
          <input
            value={query}
            onChange={event => setQuery(event.target.value)}
            placeholder="Buscar por nome ou e-mail"
          />
        </label>

        <select
          value={roleFilter}
          onChange={event =>
            setRoleFilter(event.target.value as 'ALL' | 'STUDENT' | 'ADMIN')
          }
        >
          <option value="ALL">Todos os papéis</option>
          <option value="STUDENT">STUDENT</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </div>

      <div className="admin-users-table-wrap">
        <table className="admin-users-table">
          <thead>
            <tr>
              <th>Usuário</th>
              <th>Papel</th>
              <th>Matrículas</th>
              <th>Aulas concluídas</th>
              <th>XP</th>
              <th>Cadastro</th>
              <th>Ação</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map(user => (
              <tr key={user.id}>
                <td>
                  <div className="admin-user-identity">
                    <span>{user.name.slice(0, 2).toUpperCase()}</span>
                    <div>
                      <strong>
                        {user.name}
                        {user.currentUser && <small> você</small>}
                      </strong>
                      <em>{user.email}</em>
                    </div>
                  </div>
                </td>
                <td>
                  <span className={`admin-role-badge ${user.role.toLowerCase()}`}>
                    {user.role}
                  </span>
                </td>
                <td>{user.enrollments}</td>
                <td>{user.completedLessons}</td>
                <td>{user.xp}</td>
                <td>{new Date(user.createdAt).toLocaleDateString('pt-BR')}</td>
                <td>
                  <select
                    className="admin-role-select"
                    value={user.role}
                    disabled={user.currentUser || updatingId === user.id}
                    onChange={event =>
                      void changeRole(
                        user,
                        event.target.value as 'STUDENT' | 'ADMIN'
                      )
                    }
                    title={
                      user.currentUser
                        ? 'Sua própria conta ADMIN é protegida'
                        : 'Alterar papel do usuário'
                    }
                  >
                    <option value="STUDENT">STUDENT</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
              </tr>
            ))}

            {!loading && filteredUsers.length === 0 && (
              <tr>
                <td colSpan={7}>
                  <div className="admin-users-empty">
                    Nenhum usuário encontrado com os filtros atuais.
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}

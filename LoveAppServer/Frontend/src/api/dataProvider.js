import api from './axios'

/**
 * React Admin data provider for the LoveApp Admin API.
 * Uses _start/_end pagination (ra-data-simple-rest style).
 * The admin API returns arrays + X-Total-Count header.
 */
const dataProvider = {
  getList: async (resource, { pagination, sort, filter }) => {
    const { page = 1, perPage = 25 } = pagination || {}
    const { field = 'created_at', order = 'DESC' } = sort || {}
    const _start = (page - 1) * perPage
    const _end   = page * perPage

    const params = { _start, _end, _sort: field, _order: order, ...filter }
    // Flatten filter.q for search
    if (filter?.q) params.q = filter.q

    const res = await api.get(`/admin/${resource}`, { params })
    const total = parseInt(res.headers['x-total-count'] || res.data?.length || 0)
    const data  = Array.isArray(res.data) ? res.data : res.data?.data || []

    return { data, total }
  },

  getOne: async (resource, { id }) => {
    // Some resources support single fetch
    const res = await api.get(`/admin/${resource}`, { params: { id } })
    const data = res.data?.data || (Array.isArray(res.data) ? res.data[0] : res.data)
    return { data: { ...data, id: data?.id || id } }
  },

  getMany: async (resource, { ids }) => {
    const results = await Promise.all(
      ids.map(id => api.get(`/admin/${resource}`, { params: { id } })
        .then(r => r.data?.data || r.data)
        .catch(() => ({ id })))
    )
    return { data: results }
  },

  getManyReference: async (resource, { target, id, pagination, sort, filter }) => {
    const { page = 1, perPage = 25 } = pagination || {}
    const _start = (page - 1) * perPage
    const _end   = page * perPage
    const params = { _start, _end, [target]: id, ...filter }
    const res    = await api.get(`/admin/${resource}`, { params })
    const total  = parseInt(res.headers['x-total-count'] || 0)
    return { data: Array.isArray(res.data) ? res.data : [], total }
  },

  update: async (resource, { id, data }) => {
    await api.put(`/admin/${resource}/${id}`, data)
    return { data: { ...data, id } }
  },

  updateMany: async (resource, { ids, data }) => {
    await Promise.all(ids.map(id => api.put(`/admin/${resource}/${id}`, data)))
    return { data: ids }
  },

  create: async (resource, { data }) => {
    const res = await api.post(`/admin/${resource}`, data)
    return { data: { ...data, id: res.data?.data?.id || Date.now() } }
  },

  delete: async (resource, { id }) => {
    await api.delete(`/admin/${resource}/${id}`)
    return { data: { id } }
  },

  deleteMany: async (resource, { ids }) => {
    await Promise.all(ids.map(id => api.delete(`/admin/${resource}/${id}`)))
    return { data: ids }
  },
}

export default dataProvider

const context = require.context('./module-docs', false, /\.md$/)

const docs = context.keys().reduce((result, filePath) => {
  const slug = filePath.replace('./', '').replace(/\.md$/, '')
  const doc = context(filePath)
  result[slug] = doc.default || doc
  return result
}, {})

export function getModuleDoc(slug) {
  return docs[slug] || null
}

export function hasModuleDoc(slug) {
  return Boolean(getModuleDoc(slug))
}

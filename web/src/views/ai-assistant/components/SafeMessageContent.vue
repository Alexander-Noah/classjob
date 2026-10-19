<script>
import { h } from 'vue'
import { parseMessageContent } from '../../../modules/ai-assistant/formatMessage'

export default {
  name: 'SafeMessageContent',
  props: { content: { type: String, default: '' } },
  setup(props) {
    const render = item => typeof item === 'string' ? item : h(item.tag, item.attrs, item.children.map(render))
    return () => h('div', parseMessageContent(props.content).map(render))
  },
}
</script>

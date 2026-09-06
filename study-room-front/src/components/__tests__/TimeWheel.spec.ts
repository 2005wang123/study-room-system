import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TimeWheel from '../TimeWheel.vue'

// jsdom 未实现 Element.scrollTo，补一个空实现避免组件内部报错
Object.defineProperty(HTMLElement.prototype, 'scrollTo', {
  configurable: true,
  writable: true,
  value: () => {}
})

describe('TimeWheel', () => {
  it('渲染全部档位，并高亮当前选中值', () => {
    const wrapper = mount(TimeWheel, {
      props: {
        modelValue: '10:00',
        options: ['09:00', '10:00', '11:00'],
        disabledValues: []
      }
    })
    expect(wrapper.findAll('.tw-item')).toHaveLength(3)
    expect(wrapper.find('.tw-item.is-active').text()).toBe('10:00')
  })

  it('点击可用档位触发 update:modelValue；点击禁用档位不触发', async () => {
    const wrapper = mount(TimeWheel, {
      props: {
        modelValue: '10:00',
        options: ['09:00', '10:00', '11:00', '12:00'],
        disabledValues: ['12:00']
      }
    })
    const items = wrapper.findAll('.tw-item')

    await items[0].trigger('click') // 09:00 可用
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['09:00'])

    await items[3].trigger('click') // 12:00 禁用
    expect(wrapper.emitted('update:modelValue')).toHaveLength(1)
  })

  it('无可用档位时上下箭头均禁用', () => {
    const wrapper = mount(TimeWheel, {
      props: {
        modelValue: '',
        options: ['09:00'],
        disabledValues: ['09:00']
      }
    })
    const arrows = wrapper.findAll('.tw-arrow')
    expect(arrows[0].attributes('disabled')).toBeDefined()
    expect(arrows[1].attributes('disabled')).toBeDefined()
  })
})

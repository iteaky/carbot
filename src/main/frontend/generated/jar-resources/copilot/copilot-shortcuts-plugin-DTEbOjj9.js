import { r as f, b as c, E as g, D as i, w as b, a5 as m, a1 as o, H as $ } from "./copilot-FMkXCpRK.js";
import { B as v } from "./base-panel-RVhR_MdC.js";
import { i as e } from "./icons-3ggBm36p.js";
const y = 'copilot-shortcuts-panel{display:flex;flex-direction:column;padding:var(--space-150)}copilot-shortcuts-panel h3{font:var(--copilot-font-xs-semibold);margin-bottom:var(--space-100);margin-top:0}copilot-shortcuts-panel h3:not(:first-of-type){margin-top:var(--space-200)}copilot-shortcuts-panel ul{display:flex;flex-direction:column;list-style:none;margin:0;padding:0}copilot-shortcuts-panel ul li{display:flex;align-items:center;gap:var(--space-50);position:relative}copilot-shortcuts-panel ul li:not(:last-of-type):before{border-bottom:1px dashed var(--border-color);content:"";inset:auto 0 0 calc(var(--copilot-size-md) + var(--space-50));position:absolute}copilot-shortcuts-panel ul li span:has(svg){align-items:center;display:flex;height:var(--copilot-size-md);justify-content:center;width:var(--copilot-size-md)}copilot-shortcuts-panel .kbds{margin-inline-start:auto}copilot-shortcuts-panel kbd{align-items:center;border:1px solid var(--border-color);border-radius:var(--vaadin-radius-m);box-sizing:border-box;display:inline-flex;font-family:var(--copilot-font-family);font-size:var(--copilot-font-size-xs);line-height:var(--copilot-line-height-sm);padding:0 var(--space-50)}', u = window.Vaadin.copilot.tree;
if (!u)
  throw new Error("Tried to access copilot tree before it was initialized.");
var x = Object.getOwnPropertyDescriptor, w = (t, l, h, p) => {
  for (var s = p > 1 ? void 0 : p ? x(l, h) : l, n = t.length - 1, r; n >= 0; n--)
    (r = t[n]) && (s = r(s) || s);
  return s;
};
let d = class extends v {
  constructor() {
    super(), this.onTreeUpdated = () => {
      this.requestUpdate();
    };
  }
  connectedCallback() {
    super.connectedCallback(), c.on("copilot-tree-created", this.onTreeUpdated);
  }
  disconnectedCallback() {
    super.disconnectedCallback(), c.off("copilot-tree-created", this.onTreeUpdated);
  }
  render() {
    const t = u.hasFlowComponents();
    return i`<style>
        ${y}
      </style>
      <h3>Global</h3>
      <ul>
        <li>
          <span>${e.vaadin}</span>
          <span>Copilot</span>
          ${a(o.toggleCopilot)}
        </li>
        <li>
          <span>${e.flipBack}</span>
          <span>Undo</span>
          ${a(o.undo)}
        </li>
        <li>
          <span>${e.flipForward}</span>
          <span>Redo</span>
          ${a(o.redo)}
        </li>
      </ul>
      <h3>Selected component</h3>
      <ul>
        <li>
          <span>${e.terminal}</span>
          <span>Open AI popover</span>
          ${a(o.openAiPopover)}
        </li>
        <li>
          <span>${e.fileCodeAlt}</span>
          <span>Go to source</span>
          ${a(o.goToSource)}
        </li>
        ${t ? i`<li>
              <span>${e.code}</span>
              <span>Go to attach source</span>
              ${a(o.goToAttachSource)}
            </li>` : g}
        <li>
          <span>${e.copy}</span>
          <span>Copy</span>
          ${a(o.copy)}
        </li>
        <li>
          <span>${e.clipboard}</span>
          <span>Paste</span>
          ${a(o.paste)}
        </li>
        <li>
          <span>${e.copyAlt}</span>
          <span>Duplicate</span>
          ${a(o.duplicate)}
        </li>
        <li>
          <span>${e.userUp}</span>
          <span>Select parent</span>
          ${a(o.selectParent)}
        </li>
        <li>
          <span>${e.userLeft}</span>
          <span>Select previous sibling</span>
          ${a(o.selectPreviousSibling)}
        </li>
        <li>
          <span>${e.userRight}</span>
          <span>Select first child / next sibling</span>
          ${a(o.selectNextSibling)}
        </li>
        <li>
          <span>${e.trash}</span>
          <span>Delete</span>
          ${a(o.delete)}
        </li>
        <li>
          <span>${e.zap}</span>
          <span>Quick add from palette</span>
          ${a("<kbd>A ... Z</kbd>")}
        </li>
      </ul>`;
  }
  /**
   * Closes the panel. Used from shortcuts
   */
  close() {
    b.updatePanel("copilot-shortcuts-panel", {
      floating: !1
    });
  }
};
d = w([
  f("copilot-shortcuts-panel")
], d);
function a(t) {
  return i`<span class="kbds">${m(t)}</span>`;
}
const C = $({
  header: "Keyboard Shortcuts",
  tag: "copilot-shortcuts-panel",
  width: 400,
  height: 550,
  floatingPosition: {
    top: 50,
    left: 50
  }
}), P = {
  init(t) {
    t.addPanel(C);
  }
};
window.Vaadin.copilot.plugins.push(P);

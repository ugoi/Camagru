// custom-elements-manifest.config.js

import { customElementVsCodePlugin } from "custom-element-vs-code-integration";

const options = {
  /** Output directory to write the React wrappers to - default is the root of the project */
  outdir: "dist",

  /** Name of the file with you component's custom HTML data */
  htmlFileName: "my-library.html-custom-data.json",

  /** Name of the file with you component's custom CSS data */
  cssFileName: "my-library.css-custom-data.json",

  /** class names of any components you would like to exclude from the custom data */
  exclude: ["MyInternalElement"],

  /** The property name from the component object that you would like to use for the description of your component */
  descriptionSrc: "description",

  /** Displays the slot section of the element description */
  hideSlotDocs: false,

  /** Displays the event section of the element description */
  hideEventDocs: false,

  /** Displays the CSS custom properties section of the element description */
  hideCssPropertiesDocs: false,

  /** Displays the CSS parts section of the element description */
  hideCssPartsDocs: false,

  /** Displays the methods section of the element description */
  hideMethodDocs: true,

  /** Overrides the default section labels in the component description */
  labels: {
    slots: "Slot Section",
    events: "Custom Events",
    cssProperties: "CSS Variables",
    cssParts: "Style Hooks",
    methods: "Functions",
  },

  /** Creates reusable CSS values for consistency in components */
  cssSets: [
    {
      name: "radiuses",
      values: [
        { name: "--radius-sm", description: "2px" },
        { name: "--radius-md", description: "4px" },
        { name: "--radius-lg", description: "8px" },
      ],
    },
  ],

  /** Used to create an array of links within the component info bubble */
  referencesTemplate: (name, tag) => [
    {
      name: "Documentation",
      url: `https://example.com/components/${tag}`,
    },
  ],

  /** The property form your CEM component object to display your types */
  typesSrc: "expandedType",
};

export default {
  plugins: [customElementVsCodePlugin(options)],
};

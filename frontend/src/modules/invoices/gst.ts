/**
 * GST arithmetic in integer paise so the live preview matches the server's BigDecimal
 * HALF_UP rounding: per line, taxable = qty × unitPrice, gst = round(taxable × rate / 100),
 * lineTotal = taxable + gst. Intra-state GST is split equally into CGST and SGST.
 */
export interface LineInput {
  quantity: number;
  unitPrice: number;
  gstRate: number;
}

export interface LineAmounts {
  taxable: number;
  gst: number;
  total: number;
}

export interface RateBreakdown {
  rate: number;
  taxable: number;
  cgst: number;
  sgst: number;
  gst: number;
}

export interface GstSummary {
  rates: RateBreakdown[];
  subtotal: number;
  gst: number;
  cgst: number;
  sgst: number;
  total: number;
}

const toPaise = (rupees: number) => Math.round(rupees * 100);
const toRupees = (paise: number) => paise / 100;

function linePaise({ quantity, unitPrice, gstRate }: LineInput) {
  const taxable = Math.round(quantity * toPaise(unitPrice));
  // rate in hundredths of a percent keeps 2.5 % / 0.25 % exact.
  const gst = Math.round((taxable * Math.round(gstRate * 100)) / 10_000);
  return { taxable, gst };
}

function splitPaise(gst: number) {
  const cgst = Math.round(gst / 2);
  return { cgst, sgst: gst - cgst };
}

export function lineAmounts(line: LineInput): LineAmounts {
  const { taxable, gst } = linePaise(line);
  return { taxable: toRupees(taxable), gst: toRupees(gst), total: toRupees(taxable + gst) };
}

export function summarize(lines: LineInput[]): GstSummary {
  const byRate = new Map<number, { taxable: number; gst: number }>();
  for (const line of lines) {
    const { taxable, gst } = linePaise(line);
    const acc = byRate.get(line.gstRate) ?? { taxable: 0, gst: 0 };
    byRate.set(line.gstRate, { taxable: acc.taxable + taxable, gst: acc.gst + gst });
  }
  let subtotal = 0;
  let gst = 0;
  let cgst = 0;
  const rates = [...byRate.entries()]
    .sort(([a], [b]) => a - b)
    .map(([rate, acc]) => {
      const split = splitPaise(acc.gst);
      subtotal += acc.taxable;
      gst += acc.gst;
      cgst += split.cgst;
      return { rate, taxable: toRupees(acc.taxable), cgst: toRupees(split.cgst), sgst: toRupees(split.sgst), gst: toRupees(acc.gst) };
    });
  return {
    rates,
    subtotal: toRupees(subtotal),
    gst: toRupees(gst),
    cgst: toRupees(cgst),
    sgst: toRupees(gst - cgst),
    total: toRupees(subtotal + gst),
  };
}

const INR = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });

export function formatInr(amount: number | null | undefined): string {
  return INR.format(amount ?? 0);
}
